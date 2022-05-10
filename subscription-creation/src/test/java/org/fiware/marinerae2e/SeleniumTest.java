package org.fiware.marinerae2e;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SeleniumTest {

	static WebDriver webDriver;

	@BeforeAll
	public static void setUp() throws MalformedURLException {
		String remoteDriverUrl = Optional.ofNullable(System.getenv("REMOTE_DRIVER_URL")).orElse("http://localhost:4444");
		webDriver = ChromeDriver.builder().address(URI.create(remoteDriverUrl)).build();
	}

	@Test
	public void test() {
		webDriver.get("http://grafana-demo.apps.fiware-spain.emea-1.rht-labs.com");
		assertEquals("grafana-demo.apps.fiware-spain.emea-1.rht-labs.com", webDriver.getCurrentUrl());
	}

}
