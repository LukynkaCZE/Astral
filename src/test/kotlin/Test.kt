import cz.lukynka.astral.AstralTest
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.seconds

class ExampleTest : AstralTest() {

    private lateinit var user: User
    private var userCountry: String? = null

    override fun setup() {
    }

    override fun cleanup() {
    }

    override fun createTestSteps() {

        addStep("Create user") {
            user = User("Maya", 22)
        }

        addStep("Fetch country") {
            user.fetchCountryFromIPAddress().thenAccept { country ->
                userCountry = country
            }
        }

        addWaitUntil("Wait until country is fetched", timeout = 10.seconds) { userCountry != null }

        addAssert("Country is czech republic") { userCountry == "Czech Republic" }
    }

}


data class User(val name: String, val age: Int) {
    val country: String? = null

    fun fetchCountryFromIPAddress(): CompletableFuture<String> {
        val future = CompletableFuture<String>()

        CompletableFuture.runAsync {
            Thread.sleep(3000)
            future.complete("Canada")
        }

        return future
    }
}