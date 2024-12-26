package org.springframework.samples.petclinic;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;


public class ImportantClickPaths extends BaseSpringBootTest {

	static Playwright playwright;
	static Browser browser;

	// New instance for each test method.
	BrowserContext context;
	Page page;

	@BeforeEach
	void createContextAndPage() {
		context = browser.newContext(new Browser.NewContextOptions()
			.setRecordVideoDir(Paths.get("build/test-results/videos/"))
			.setRecordVideoSize(640, 480)
			.setLocale("de"));
		context.setDefaultTimeout(10000);
		page = context.newPage();
	}

	@AfterEach
	void closeContext() {
		context.close();
	}

	@Test
	void ownerIsCreated() {
		page.navigate("http://localhost:" + port + "/owners");
		page.locator("#nav-item-search").click();
		page.locator("#lastName").fill("asd");
		page.locator("#search-owner-form-submit").click();

		assertThat(page.content()).contains("wurde nicht gefunden");

		fillOutOwnerFormAndSubmit();

		page.locator("#edit-owner").click();
		page.getByRole(AriaRole.TEXTBOX).and(page.locator("#firstName")).fill("Paul");
		page.locator("#submit-owner").click();

		assertThat(page.content()).contains("Paul");
	}
	@Test
	void petForOwnerIsCreated() {
		page.navigate("http://localhost:" + port + "/owners");
		page.locator("#nav-item-search").click();
		fillOutOwnerFormAndSubmit();

		page.locator("#add-pet-to-owner").click();

		page.getByRole(AriaRole.TEXTBOX).and(page.locator("#name")).fill("buddy");

		page.locator("#submit-pet").click();
		assertThat(page.content()).contains("New Pet has been Added");

		page.locator("#edit-pet").click();
		page.getByRole(AriaRole.TEXTBOX).and(page.locator("#name")).fill("Rex");
		page.locator("#submit-pet").click();

		assertThat(page.content()).contains("Rex");
	}
	@Test
	void visitsForPetAreCreated() {
		page.navigate("http://localhost:" + port + "/owners");
		page.locator("#nav-item-search").click();
		fillOutOwnerFormAndSubmit();

		page.locator("#add-pet-to-owner").click();

		page.getByRole(AriaRole.TEXTBOX).and(page.locator("#name")).fill("buddy");

		page.locator("#submit-pet").click();
		assertThat(page.content()).contains("New Pet has been Added");

		page.locator("#add-visit-to-pet").click();
		page.getByRole(AriaRole.TEXTBOX).and(page.locator("#description")).fill("foo");
		page.locator("#add-visit").click();
		assertThat(page.content()).contains("Your visit has been booked");
		assertThat(page.content()).contains("foo");

		page.locator("#add-visit-to-pet").click();
		page.getByRole(AriaRole.TEXTBOX).and(page.locator("#description")).fill("bar");
		page.locator("#add-visit").click();
		assertThat(page.content()).contains("Your visit has been booked");
		assertThat(page.content()).contains("foo");
		assertThat(page.content()).contains("bar");
	}

	private void fillOutOwnerFormAndSubmit() {
		page.locator("#search-owner-form > a").click();
		page.locator("#firstName").type("asd");
		page.locator("#lastName").type("def");
		page.locator("#address").type("foo");
		page.locator("#city").type("bar");
		page.locator("#telephone").type("1234567890");

		page.locator("#submit-owner").click();
		assertThat(page.content()).contains("asd def");
	}

	@BeforeAll
	static void launchBrowser() {
		playwright = Playwright.create();
		browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
	}

	@AfterAll
	static void closeBrowser() {
		playwright.close();
	}
}
