package org.springframework.samples.petclinic

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType.LaunchOptions
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.AriaRole
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.nio.file.Paths

class ImportantClickPaths : BaseAppTest() {
    // New instance for each test method.
    var context: BrowserContext? = null
    var page: Page? = null

    @BeforeEach
    fun createContextAndPage() {
        context = browser!!.newContext(
            Browser.NewContextOptions()
                .setRecordVideoDir(Paths.get("build/test-results/videos/"))
                .setRecordVideoSize(640, 480)
                .setLocale("de")
        ).apply {
            setDefaultTimeout(10000.0)
            page = newPage()
        }
    }

    @AfterEach
    fun closeContext() {
        context!!.close()
    }

    @Test
    fun ownerIsCreated() {
        page!!.navigate("http://localhost:$port/owners")
        page!!.locator("#nav-item-search").click()
        page!!.locator("#lastName").fill("asd")
        page!!.locator("#search-owner-form-submit").click()

        assertThat(page!!.content()).contains("wurde nicht gefunden")

        fillOutOwnerFormAndSubmit()

        page!!.locator("#edit-owner").click()
        page!!.getByRole(AriaRole.TEXTBOX).and(page!!.locator("#firstName")).fill("Paul")
        page!!.locator("#submit-owner").click()

        assertThat(page!!.content()).contains("Paul")
    }

    @Test
    fun petForOwnerIsCreated() {
        page!!.navigate("http://localhost:$port/owners")
        page!!.locator("#nav-item-search").click()
        fillOutOwnerFormAndSubmit()

        page!!.locator("#add-pet-to-owner").click()

        page!!.getByRole(AriaRole.TEXTBOX).and(page!!.locator("#name")).fill("buddy")

        page!!.locator("#submit-pet").click()
        assertThat(page!!.content()).contains("New Pet has been Added")

        page!!.locator("#edit-pet").click()
        page!!.getByRole(AriaRole.TEXTBOX).and(page!!.locator("#name")).fill("Rex")
        page!!.locator("#submit-pet").click()

        assertThat(page!!.content()).contains("Rex")
    }

    @Test
    fun visitsForPetAreCreated() {
        page!!.navigate("http://localhost:$port/owners")
        page!!.locator("#nav-item-search").click()
        fillOutOwnerFormAndSubmit()

        page!!.locator("#add-pet-to-owner").click()

        page!!.getByRole(AriaRole.TEXTBOX).and(page!!.locator("#name")).fill("buddy")

        page!!.locator("#submit-pet").click()
        assertThat(page!!.content()).contains("New Pet has been Added")

        page!!.locator("#add-visit-to-pet").click()
        page!!.getByRole(AriaRole.TEXTBOX).and(page!!.locator("#description")).fill("foo")
        page!!.locator("#add-visit").click()
        assertThat(page!!.content()).contains("Your visit has been booked")
        assertThat(page!!.content()).contains("foo")

        page!!.locator("#add-visit-to-pet").click()
        page!!.getByRole(AriaRole.TEXTBOX).and(page!!.locator("#description")).fill("bar")
        page!!.locator("#add-visit").click()
        assertThat(page!!.content()).contains("Your visit has been booked")
        assertThat(page!!.content()).contains("foo")
        assertThat(page!!.content()).contains("bar")
    }

    private fun fillOutOwnerFormAndSubmit() {
        page!!.locator("#search-owner-form > a").click()
        page!!.locator("#firstName").type("asd")
        page!!.locator("#lastName").type("def")
        page!!.locator("#address").type("foo")
        page!!.locator("#city").type("bar")
        page!!.locator("#telephone").type("1234567890")

        page!!.locator("#submit-owner").click()
        assertThat(page!!.content()).contains("asd def")
    }

    companion object {
        var playwright: Playwright? = null
        var browser: Browser? = null

        @BeforeAll
        @JvmStatic
        fun launchBrowser() {
            playwright = Playwright.create().apply {
                browser = chromium().launch(LaunchOptions().setHeadless(true))
            }
        }

        @AfterAll
        @JvmStatic
        fun closeBrowser() {
            playwright?.close()
        }
    }
}
