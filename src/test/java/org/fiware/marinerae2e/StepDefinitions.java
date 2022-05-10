package org.fiware.marinerae2e;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StepDefinitions {

	private static String remoteDriverUrl;
	private static String grafanaUrl;
	private static String brokerUrl;

	private static String grafanaUser;
	private static String grafanaPassword;


	static WebDriver webDriver;

	@Before
	public void setUp() throws MalformedURLException {
		remoteDriverUrl = Optional.ofNullable(System.getenv("REMOTE_DRIVER_URL")).orElse("http://localhost:4444");
		grafanaUrl = Optional.ofNullable(System.getenv("GRAFANA_URL")).orElse("http://localhost:3000");
		brokerUrl = Optional.ofNullable(System.getenv("BROKER_URL")).orElse("http://localhost:1026");

		grafanaUser = Optional.ofNullable(System.getenv("GRAFANA_USERNAME")).orElse("fiwareAdmin");
		grafanaPassword = Optional.ofNullable(System.getenv("GRAFANA_PASSWORD")).orElse("fiwareAdmin");

		webDriver = ChromeDriver.builder().address(URI.create(remoteDriverUrl)).build();
	}

	@When("A user opens Grafana.")
	public void verify_forward_to_login_page() {
		webDriver.get(grafanaUrl);
		assertEquals(String.format("%s/login", grafanaUrl), webDriver.getCurrentUrl(), "The user should be forwarded to the login-page.");
	}

	@When("The user logs into Grafana as an admin.")
	public void login_to_grafana_as_admin() {
		WebElement userInput = webDriver.findElement(By.name("user"));
		WebElement passwordInput = webDriver.findElement(By.name("password"));
		WebElement loginButton = webDriver.findElement(By.className("css-6sxr68-button"));
		userInput.sendKeys(grafanaUser);
		passwordInput.sendKeys(grafanaPassword);

		loginButton.click();
		assertEquals("Grafana", webDriver.getTitle(), "The user should now be logged into Grafana.");
	}

	@When("The user navigates to the dashboard.")
	public void move_to_dashboard() {
		webDriver.get(String.format("%s/d/orion-datasource-checker/orion-datasource-checker?orgId=1", grafanaUrl));
		WebElement panelHeader = webDriver.findElement(By.cssSelector("div.panel-header:nth-child(2) > header:nth-child(1) > div:nth-child(1) > h2:nth-child(1)"));
		assertEquals("Timescale DB", panelHeader.getText(), "The timescale db panel should be visible.");
	}

	@After
	public void cleanUp() {
		webDriver.close();
	}

}
