package org.fiware.marinerae2e;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.AccessTokenResponse;
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
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class StepDefinitions {

	public String fiwareService = "AirQuality";
	public String fiwareServicePath = "/alcantarilla";

	// name of the test entity to be created
	private String testEntityId;
	// the test relies on the datasource-checker dashboard. It needs to be available under the configured name.
	private String datasourceCheckerDashboardName;
	// the position of the tables inside the datagrid. The dashboard to test should have at least 2 tables:
	// Historic data and current data
	private String historicDataGridPosition;
	private String currentDataGridPosition;

	// url of the selenium instance to be used
	private String remoteDriverUrl;
	// url of the grafana to check
	private String grafanaUrl;
	// url of the context broker to be used for data creation
	private String brokerUrl;
	// url of quantum leap to send the notifications to
	private String quantumLeapUrl;
	// username of the grafana(admin)-user to use
	private String grafanaUser;
	// password of the grafana(admin)-user to use
	private String grafanaPassword;

	// KEYCLOAK
	private String keycloakUsername;
	private String keycloakPassword;
	private String keycloakClientId;
	private String keycloakClientSecret;
	private String keycloakURL;
	private String keycloakRealm;

	// holds the test subscription's location for later cleanup
	private Optional<String> subscriptionLocation = Optional.empty();

	// Air Quality Application variables
	private String airQualityDataMonitorDashboardName;
	private String airQualityHome;

	// instance of the webdriver for contacting selenium
	private static WebDriver webDriver;

	private Optional<TokenManager> optionalTokenManager = Optional.empty();

	/**
	 * Reads the env and creates the connection to selenium.
	 */
	@Before
	public void setUp() throws MalformedURLException {

		readEnv();

		ChromeOptions chromeOptions = new ChromeOptions();
		webDriver = new RemoteWebDriver(new URL(remoteDriverUrl), chromeOptions);
		webDriver.manage().timeouts().implicitlyWait(Duration.of(5, ChronoUnit.SECONDS));
	}

	/**
	 * Read all env vars and set them. Else use the default values.
	 */
	private void readEnv() {
		remoteDriverUrl = Optional.ofNullable(System.getenv("REMOTE_DRIVER_URL")).orElse("http://localhost:4444");
		grafanaUrl = Optional.ofNullable(System.getenv("GRAFANA_URL")).orElse("http://localhost:3000");
		brokerUrl = Optional.ofNullable(System.getenv("BROKER_URL")).orElse("http://localhost:1026");
		quantumLeapUrl = Optional.ofNullable(System.getenv("QUANTUM_LEAP_URL")).orElse("http://quantumleap-quantumleap:8668");

		testEntityId = Optional.ofNullable(System.getenv("TEST_ENTITY_ID")).orElse("test-air-quality");
		datasourceCheckerDashboardName = Optional.ofNullable(System.getenv("DATASOURCE_CHECKER_DASHBOARD_NAME")).orElse("orion-datasource-checker");

		historicDataGridPosition = Optional.ofNullable(System.getenv("HISTORIC_DATA_GRID_POSITION")).orElse("1");
		currentDataGridPosition = Optional.ofNullable(System.getenv("CURRENT_DATA_GRID_POSITION")).orElse("2");

		grafanaUser = Optional.ofNullable(System.getenv("GRAFANA_USERNAME")).orElse("user");
		grafanaPassword = Optional.ofNullable(System.getenv("GRAFANA_PASSWORD")).orElse("password");

		fiwareService = Optional.ofNullable(System.getenv("FIWARE_SERVICE")).orElse("AirQuality");
		fiwareServicePath = Optional.ofNullable(System.getenv("FIWARE_SERVICE_PATH")).orElse("/alcantarilla");

		airQualityDataMonitorDashboardName = Optional.ofNullable(System.getenv("AIR_QUALITY_DATA_MONITOR_DASHBOARD_NAME")).orElse("air-quality-data-monitor");
		airQualityHome = Optional.ofNullable(System.getenv("AIR_QUALITY_HOME")).orElse("aqapp-home");
	}

	@Given("The keycloak connection is setup.")
	public void setupSecurity() {
		keycloakUsername = Optional.ofNullable(System.getenv("KEYCLOAK_USERNAME")).orElseThrow(() -> new RuntimeException("A username is required for the pep-flow."));
		keycloakPassword = Optional.ofNullable(System.getenv("KEYCLOAK_PASSWORD")).orElseThrow(() -> new RuntimeException("A password is required for the pep-flow."));
		keycloakClientId = Optional.ofNullable(System.getenv("KEYCLOAK_CLIENT_ID")).orElseThrow(() -> new RuntimeException("A client-id is required for the pep-flow."));
		keycloakClientSecret = Optional.ofNullable(System.getenv("KEYCLOAK_CLIENT_SECRET")).orElseThrow(() -> new RuntimeException("A client-secret is required for the pep-flow."));
		keycloakURL = Optional.ofNullable(System.getenv("KEYCLOAK_URL")).orElseThrow(() -> new RuntimeException("URL of keycloak is required for the pep-flow."));
		keycloakRealm = Optional.ofNullable(System.getenv("KEYCLOAK_REALM")).orElseThrow(() -> new RuntimeException("A realm is required for the pep-flow."));
		brokerUrl = Optional.ofNullable(System.getenv("PEP_URL")).orElseThrow(() -> new RuntimeException("A url to the pep-proxy needs to be provided."));
		optionalTokenManager = getOptionalTokenManager();
	}

	@When("A user opens Grafana.")
	public void verify_forward_to_login_page() {
		webDriver.get(grafanaUrl);
		assertEquals(String.format("%s/login", grafanaUrl), webDriver.getCurrentUrl(), "The user should be forwarded to the login-page.");
	}

	@Given("The subscription between Orion-LD and QuantumLeap is created.")
	public void create_subscription() throws IOException {
		OkHttpClient brokerClient = new OkHttpClient();

		RequestBody subscriptionBody = RequestBody.create(getSubscriptionString(quantumLeapUrl, fiwareService, fiwareServicePath), MediaType.get("application/json"));

		Request subscriptionCreationRequest = new Request.Builder()
				.url(String.format("%s/v2/subscriptions", brokerUrl))
				.addHeader("Fiware-Service", fiwareService)
				.addHeader("Fiware-ServicePath", fiwareServicePath)
				.addHeader("Authorization", String.format("bearer %s", optionalTokenManager.map(TokenManager::getAccessToken).map(AccessTokenResponse::getToken).orElse("noToken")))
				.method("POST", subscriptionBody)
				.build();
		Response response = brokerClient.newCall(subscriptionCreationRequest).execute();
		assertTrue(response.code() >= 200 && response.code() < 300, "We expect any kind of successful response.");
		// store for better cleanup
		subscriptionLocation = Optional.of(response.header("Location"));
	}

	private Optional<TokenManager> getOptionalTokenManager() {

		return Optional.of(KeycloakBuilder.builder()
				.username(keycloakUsername)
				.password(keycloakPassword)
				.clientSecret(keycloakClientSecret)
				.clientId(keycloakClientId)
				.grantType("password")
				.realm(keycloakRealm)
				.serverUrl(keycloakURL)
				.build()
				.tokenManager());
	}

	@Given("Some AirQuality data is created.")
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
			Instant historicalNow = now.minus(Duration.of(i, ChronoUnit.MINUTES));
			RequestBody entityBody = RequestBody.create(getTestEntity(testEntityId, temp, humidity, co, no2, formatter.format(historicalNow)), MediaType.get("application/json"));
			Request entityCreationRequest = new Request.Builder()
					.url(orionUrl)
					.addHeader("Fiware-Service", fiwareService)
					.addHeader("Fiware-ServicePath", fiwareServicePath)
					.addHeader("Authorization", String.format("bearer %s", optionalTokenManager.map(TokenManager::getAccessToken).map(AccessTokenResponse::getToken).orElse("noToken")))
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
		webDriver.get(String.format("%s/d/%s/%s?orgId=1", grafanaUrl, datasourceCheckerDashboardName, datasourceCheckerDashboardName));

		WebDriverWait waitAfterNavigate = new WebDriverWait(webDriver, Duration.of(15, ChronoUnit.SECONDS));
		waitAfterNavigate.until(ExpectedConditions.titleIs("Orion datasource checker - Grafana"));

		WebElement panelHeader = webDriver.findElement(By.cssSelector("section.panel-container > div:nth-child(1) > header:nth-child(1) > div:nth-child(1) > h2:nth-child(1)"));
		assertEquals("Timescale DB", panelHeader.getText(), "The timescale db panel should be visible.");
	}

	@Then("The current air-quality data should be visible.")
	public void verify_current_data_is_visible() {

		List<WebElement> tableRowElements = webDriver.findElements(By.cssSelector(String.format(".table-panel-table > tbody:nth-child(%s) > tr:nth-child(1)", currentDataGridPosition)));
		assertEquals(1, tableRowElements.size(), "The entity should be there exactly once.");
		assertEquals(testEntityId, tableRowElements.get(0).findElements(By.tagName("td")).get(0).getText(), "The entry should be the test entity.");

	}

	@Then("The air-quality data history should be visible.")
	public void verify_historic_data_is_visible() {

		WebElement historicDataTable = webDriver.findElement(By.cssSelector(String.format("div.react-grid-item:nth-child(%s)", historicDataGridPosition)));

		await("Multiple entries for the test entity should exist in the historic table.")
				.atMost(Duration.of(5, ChronoUnit.SECONDS))
				.until(() -> historicDataTable.findElements(By.xpath(String.format("//*[contains(text(), '%s')]", testEntityId))).size() > 1);
	}

	@Given("Grafana is deployed.")
	public void check_grafana_is_deployed() throws IOException {

		URL url = new URL(grafanaUrl);
		OkHttpClient grafanaClient = new OkHttpClient();
		Request checkGrafana = new Request.Builder().url(url).build();
		Response response = grafanaClient.newCall(checkGrafana).execute();

		assertEquals(200, response.code());

	}

	@Then("The user should be able to navigate to Air Quality Data Monitor dashboard.")
	public void move_to_aqapp_data_monitor_dashboard() {

		webDriver.get(String.format("%s/d/%s/%s?orgId=1", grafanaUrl, airQualityHome, airQualityDataMonitorDashboardName));

		WebDriverWait waitAfterNavigate = new WebDriverWait(webDriver, Duration.of(15, ChronoUnit.SECONDS));
		waitAfterNavigate.until(ExpectedConditions.titleIs("Air Quality Data Monitor - Grafana"));

		WebElement panelHeader = webDriver.findElement(By.cssSelector(".dashboard-title > h1"));
		assertEquals("AIR QUALITY DATA MONITOR", panelHeader.getText(), "AIR QUALITY DATA MONITOR");
	}

	@Then("The user should be able to navigate to Air Quality Index \\(ICA) dashboard.")
	public void move_to_aqapp_index_dashboard() {

		List<WebElement> buttonContainerList = webDriver.findElements(By.cssSelector(".button-container"));
		buttonContainerList.get(0).click();

		WebDriverWait waitAfterNavigate = new WebDriverWait(webDriver, Duration.of(15, ChronoUnit.SECONDS));
		waitAfterNavigate.until(ExpectedConditions.titleIs("Air Quality Index (ICA) - Grafana"));

		WebElement panelHeader = webDriver.findElement(By.cssSelector(".dashboard-title > h1"));
		assertEquals("AIR QUALITY INDEX (ICA)", panelHeader.getText(), "AIR QUALITY INDEX (ICA)");

	}

	@Then("The user should be able to return to Air Quality Data Monitor dashboard.")
	public void return_to_aqapp_data_monitor_dashboard() {

		WebElement backButton = webDriver.findElement(By.cssSelector(".back-container-icon"));
		backButton.click();

		WebDriverWait waitAfterNavigate = new WebDriverWait(webDriver, Duration.of(15, ChronoUnit.SECONDS));
		waitAfterNavigate.until(ExpectedConditions.titleIs("Air Quality Data Monitor - Grafana"));

	}

	@Then("The user should be able to navigate to Air Quality - Pollutants dashboard.")
	public void move_to_aqapp_pollutants_dashboard() {

		List<WebElement> buttonContainerList = webDriver.findElements(By.cssSelector(".button-container"));
		buttonContainerList.get(1).click();

		WebDriverWait waitAfterNavigate = new WebDriverWait(webDriver, Duration.of(15, ChronoUnit.SECONDS));
		waitAfterNavigate.until(ExpectedConditions.titleIs("Air Quality - Pollutants - Grafana"));

		WebElement panelHeader = webDriver.findElement(By.cssSelector(".dashboard-title > h1"));
		assertEquals("AIR QUALITY - POLLUTANTS", panelHeader.getText(), "AIR QUALITY - POLLUTANTS");

	}

	@Then("The user should be able to navigate to Air Quality - Particulate matter dashboard.")
	public void move_to_aqapp_particulate_matter_dashboard() {

		List<WebElement> buttonContainerList = webDriver.findElements(By.cssSelector(".button-container"));
		buttonContainerList.get(2).click();

		WebDriverWait waitAfterNavigate = new WebDriverWait(webDriver, Duration.of(15, ChronoUnit.SECONDS));
		waitAfterNavigate.until(ExpectedConditions.titleIs("Air Quality - Particulate Matter - Grafana"));

		WebElement panelHeader = webDriver.findElement(By.cssSelector(".dashboard-title > h1"));
		assertEquals("AIR QUALITY - PARTICULATE MATTER", panelHeader.getText(), "AIR QUALITY - PARTICULATE MATTER");
	}

	/**
	 * Removes all artifacts created and quits the connection to selenium.
	 * Error cases:
	 * - if selenium connection is not properly closed, selenium will not be available for new runs(e.g. needs to be restarted)
	 * - if one of the cleanup steps fails, the other will still run, but the test will be marked as a failure
	 */
	@After
	public void cleanUp() throws MalformedURLException {
		Optional<Response> subDeletionResponse = Optional.empty();
		Optional<Response> qlDeletionResponse = Optional.empty();
		Optional<Response> entityDeletionResponse = Optional.empty();

		// cleanUp directly at the broker, to not fail in case of broken policies
		brokerUrl = Optional.ofNullable(System.getenv("BROKER_URL")).orElse("http://localhost:1026");

		OkHttpClient httpClient = new OkHttpClient();
		if (subscriptionLocation.isPresent()) {

			// remove subscription from the broker
			Request subscriptionDeletion = new Request.Builder()
					.url(String.format("%s/%s", brokerUrl, subscriptionLocation.get()))
					.method("DELETE", null)
					.addHeader("Fiware-Service", fiwareService)
					.addHeader("Fiware-ServicePath", fiwareServicePath)
					.addHeader("Authorization", String.format("bearer %s", optionalTokenManager.map(TokenManager::getAccessToken).map(AccessTokenResponse::getToken).orElse("noToken")))
					.build();
			try {
				subDeletionResponse = Optional.of(httpClient.newCall(subscriptionDeletion).execute());
			} catch (Exception e) {

			}
		}

		URL ql = new URL(quantumLeapUrl);

		HttpUrl qlUrl = new HttpUrl.Builder()
				.host(ql.getHost())
				.port(ql.getPort())
				.scheme(ql.getProtocol())
				.addPathSegment("v2")
				.addPathSegment("entities")
				.addPathSegment(testEntityId)
				.addEncodedQueryParameter("type", "AirQualityObserved")
				.build();

		// remove all the data from quantum leap
		Request qlDeletion = new Request.Builder()
				.url(qlUrl)
				.addHeader("Fiware-Service", fiwareService)
				.addHeader("Fiware-ServicePath", fiwareServicePath)
				.addHeader("Authorization", String.format("bearer %s", optionalTokenManager.map(TokenManager::getAccessToken).map(AccessTokenResponse::getToken).orElse("noToken")))
				.method("DELETE", null)
				.build();
		try {
			qlDeletionResponse = Optional.of(httpClient.newCall(qlDeletion).execute());
		} catch (Exception e) {
		}
		// remove data from the broker
		Request entityDeletion = new Request.Builder()
				.url(String.format("%s/v2/entities/%s", brokerUrl, testEntityId))
				.method("DELETE", null)
				.addHeader("Fiware-Service", fiwareService)
				.addHeader("Fiware-ServicePath", fiwareServicePath)
				.addHeader("Authorization", String.format("bearer %s", optionalTokenManager.map(TokenManager::getAccessToken).map(AccessTokenResponse::getToken).orElse("noToken")))
				.build();
		try {
			entityDeletionResponse = Optional.of(httpClient.newCall(entityDeletion).execute());
		} catch (Exception e) {
		}
		webDriver.quit();

		if (entityDeletionResponse.map(r -> !r.isSuccessful() && r.code() != 404).orElse(false) ||
				qlDeletionResponse.map(r -> !r.isSuccessful() && r.code() != 404).orElse(false) ||
				(subDeletionResponse.map(r -> !r.isSuccessful()).orElse(false) && subscriptionLocation.isPresent())) {

			fail(String.format("Cleanup was not successfull: Entity: %s - QL: %s - Subscription: %s",
					entityDeletionResponse.map(Response::code).map(String::valueOf).orElse("No response"),
					qlDeletionResponse.map(Response::code).map(String::valueOf).orElse("No response"),
					subDeletionResponse.map(Response::code).map(String::valueOf).orElse("No response")));
		}
	}

	private String getSubscriptionString(String quantumLeapAddress, String fiwareService, String fiwareServicePath) {
		return String.format("{\n" +
				"    \"description\": \"AQ - Orion to QuantumLeap subscription\",\n" +
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
				"        \"httpCustom\": {\n" +
				"            \"url\": \"%s/v2/notify\",\n" +
				"        	 \"headers\" : { " +
				"				\"fiware-service\" : \"%s\",		" +
				"				\"fiware-servicepath\":\"%s\"" +
				"			 }\n " +
				"        },\n" +
				"        \"attrs\": [\n" +
				"            \"CO\",\n" +
				"            \"NO2\",\n" +
				"            \"humidity\",\n" +
				"            \"temperature\"\n" +
				"        ],\n" +
				"        \"metadata\": [\n" +
				"            \"dateCreated\",\n" +
				"            \"dateModified\",\n" +
				"            \"TimeInstant\"\n" +
				"        ]\n" +
				"    }\n" +
				"}", quantumLeapAddress, fiwareService, fiwareServicePath);
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
