package org.fiware.marinerae2e;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StepDefinitions {

	private String remoteDriverUrl;
	private String grafanaUrl;
	private String brokerUrl;
	private String quantumLeapUrl;
	private String testTenant;

	private String grafanaUser;
	private String grafanaPassword;

	private String subscriptionLocation;

	static WebDriver webDriver;

	@When("A user opens Grafana.")
	public void verify_forward_to_login_page() {
		webDriver.get(grafanaUrl);
		assertEquals(String.format("%s/login", grafanaUrl), webDriver.getCurrentUrl(), "The user should be forwarded to the login-page.");
	}

	@Before
	public void setUp() throws MalformedURLException {
		remoteDriverUrl = Optional.ofNullable(System.getenv("REMOTE_DRIVER_URL")).orElse("http://localhost:4444");
		grafanaUrl = Optional.ofNullable(System.getenv("GRAFANA_URL")).orElse("http://localhost:3000");
		brokerUrl = Optional.ofNullable(System.getenv("BROKER_URL")).orElse("http://localhost:1026");
		quantumLeapUrl = Optional.ofNullable(System.getenv("QUANTUM_LEAP_URL")).orElse("http://quantumleap-quantumleap:8668");
		testTenant = Optional.ofNullable(System.getenv("TEST_TENANT")).orElse("test-tenant");

		grafanaUser = Optional.ofNullable(System.getenv("GRAFANA_USERNAME")).orElse("fiwareAdmin");
		grafanaPassword = Optional.ofNullable(System.getenv("GRAFANA_PASSWORD")).orElse("fiwareAdmin");
		ChromeOptions chromeOptions = new ChromeOptions();
		webDriver = new RemoteWebDriver(new URL(remoteDriverUrl), chromeOptions);
		webDriver.manage().timeouts().implicitlyWait(Duration.of(5, ChronoUnit.SECONDS));
	}

	@Given("The subscription between Orion-LD and QuantumLeap is created.")
	public void create_subscription() throws IOException {
		OkHttpClient brokerClient = new OkHttpClient();

		RequestBody subscriptionBody = RequestBody.create(getSubscriptionString(testTenant, quantumLeapUrl), MediaType.get("application/json"));

		Request subscriptionCreationRequest = new Request.Builder()
				.url(String.format("%s/v2/subscriptions", brokerUrl))
				.addHeader("Fiware-Service", "AirQuality")
				.addHeader("Fiware-ServicePath", "/alcantarilla")
				.method("POST", subscriptionBody)
				.build();
		Response response = brokerClient.newCall(subscriptionCreationRequest).execute();
		assertTrue(response.code() >= 200 && response.code() < 300, "We expect any kind of successful response.");
		// store for better cleanup
		subscriptionLocation = response.header("Location");
	}

	@Given("Historical data is created.")
	public void push_historical_data_to_orion() throws IOException {
		Instant now = Instant.now();
		String patternFormat = "yyyy-MM-dd'T'hh:mm:ssZ";
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(patternFormat)
				.withZone(ZoneId.systemDefault());
		URL url = new URL(brokerUrl);
		OkHttpClient brokerClient = new OkHttpClient();
		int twoDaysInMinutes = 48 * 60;
		// send in 5 minute steps
		for (int i = 0; i < twoDaysInMinutes; i += 5) {
			double temp = Math.random() * 20;
			double humidity = Math.random() * 20;
			double co = Math.random();
			double no2 = Math.random();

			HttpUrl orionUrl = new HttpUrl.Builder()
					.host(url.getHost())
					.port(url.getPort())
					.scheme(url.getProtocol())
					.addPathSegment("v2")
					.addPathSegment("entities")
					.addEncodedQueryParameter("options", "upsert")
					.build();
			Instant historicalNow = now.minus(Duration.of(5, ChronoUnit.MINUTES));
			RequestBody entityBody = RequestBody.create(getTestEntity("test-air-quality", temp, humidity, co, no2, formatter.format(historicalNow)), MediaType.get("application/json"));
			Request entityCreationRequest = new Request.Builder()
					.url(orionUrl)
					.addHeader("Fiware-Service", "AirQuality")
					.addHeader("Fiware-ServicePath", "/alcantarilla")
					.method("POST", entityBody)
					.build();
			Response response = brokerClient.newCall(entityCreationRequest).execute();
			assertTrue(response.code() >= 200 && response.code() < 300, "We expect any kind of successful response.");
		}
	}

	@When("The user logs into Grafana as an admin.")
	public void login_to_grafana_as_admin() throws InterruptedException {
		WebElement userInput = webDriver.findElement(By.name("user"));
		WebElement passwordInput = webDriver.findElement(By.name("password"));
		WebElement loginButton = webDriver.findElement(By.className("css-6sxr68-button"));

		userInput.sendKeys(grafanaUser);
		passwordInput.sendKeys(grafanaPassword);

		loginButton.click();

		WebDriverWait waitAfterLogin = new WebDriverWait(webDriver, Duration.of(15, ChronoUnit.SECONDS));
		waitAfterLogin.until(ExpectedConditions.titleIs("Home - Grafana"));
		assertEquals("Home - Grafana", webDriver.getTitle(), "The user should now be on the home-screen.");
	}

	@When("The user navigates to the dashboard.")
	public void move_to_dashboard() {
		webDriver.get(String.format("%s/d/orion-datasource-checker/orion-datasource-checker?orgId=1", grafanaUrl));
		await("The user should now be at the orion-checker dashboard.").atMost(Duration.of(5, ChronoUnit.SECONDS)).until(() -> webDriver.getTitle().equals("Orion datasource checker - Grafana"));

		WebElement panelHeader = webDriver.findElement(By.cssSelector("div.panel-header:nth-child(2) > header:nth-child(1) > div:nth-child(1) > h2:nth-child(1)"));
		assertEquals("Timescale DB", panelHeader.getText(), "The timescale db panel should be visible.");
	}

	@After
	public void cleanUp() {
		webDriver.quit();
	}

	private String getSubscriptionString(String tenant, String quantumLeapAddress) {
		return String.format("{\n" +
				"    \"description\": \"%s AQ - Orion to QuantumLeap subscription\",\n" +
				"    \"subject\": {\n" +
				"        \"entities\": [\n" +
				"            {\n" +
				"                \"idPattern\": \".*\",\n" +
				"                \"type\": \"AirQualityObserved\"\n" +
				"            }\n" +
				"        ],\n" +
				"        \"condition\": {\n" +
				"            \"attrs\": [\n" +
				"                \"temperature\"\n" +
				"            ]\n" +
				"        }\n" +
				"    },\n" +
				"    \"notification\": {\n" +
				"        \"http\": {\n" +
				"            \"url\": \"%s/v2/notify\"\n" +
				"        },\n" +
				"        \"attrs\": [\n" +
				"            \"CO\",\n" +
				"            \"NO2\",\n" +
				"\n" +
				"            \"O3\",\n" +
				"            \"SO2\",\n" +
				"            \"humidity\",\n" +
				"            \"temperature\"\n" +
				"        ],\n" +
				"        \"metadata\": [\n" +
				"            \"dateCreated\",\n" +
				"            \"dateModified\",\n" +
				"            \"TimeInstant\"\n" +
				"        ]\n" +
				"    }\n" +
				"}", tenant, quantumLeapAddress);
	}

	private String getTestEntity(String id, double temperature, double humidity, double co, double no2, String timeInstant) {
		return String.format("{\n" +
				"  \"type\": \"AirQualityObserved\",\n" +
				"  \"id\": \"%s\",\n" +
				"  \"temperature\": {\n" +
				"  	 \"type\": \"Number\", " +
				"    \"value\": %s,\n" +
				"    \"metadata\": {" +
				"		\"TimeInstant\": {" +
				" 			\"type\":\"DateTime\", " +
				" 			\"value\":\"%s\" " +
				" 		}\n" +
				"	 }\n" +
				"  },\n" +
				"  \"humidity\": {\n" +
				"  	 \"type\": \"Number\", " +
				"    \"value\": %s,\n" +
				"    \"metadata\": {" +
				"		\"TimeInstant\": {" +
				" 			\"type\":\"DateTime\", " +
				" 			\"value\":\"%s\" " +
				" 		}\n" +
				"	 }\n" +
				"  },\n" +
				"  \"co\": {\n" +
				"  	 \"type\": \"Number\", " +
				"    \"value\": %s,\n" +
				"    \"metadata\": {" +
				"		\"TimeInstant\": {" +
				" 			\"type\":\"DateTime\", " +
				" 			\"value\":\"%s\" " +
				" 		}\n" +
				"	 }\n" +
				"  },\n" +
				"  \"no2\": {\n" +
				"  	 \"type\": \"Number\", " +
				"    \"value\": %s,\n" +
				"    \"metadata\": {" +
				"		\"TimeInstant\": {" +
				" 			\"type\":\"DateTime\", " +
				" 			\"value\":\"%s\" " +
				" 		}\n" +
				"	 }\n" +
				"  }\n" +
				"}", id, temperature, timeInstant, humidity, timeInstant, co, timeInstant, no2, timeInstant);
	}
}
