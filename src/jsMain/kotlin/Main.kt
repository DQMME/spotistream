import kotlinx.browser.document
import page.commands
import page.rewards
import page.setup
import react.create
import react.dom.client.createRoot

fun main() {
    val commandsElement = document.getElementById("commands")
    if(commandsElement != null) createRoot(commandsElement).render(commands.create())

    val rewardsElement = document.getElementById("rewards")
    if(rewardsElement != null) createRoot(rewardsElement).render(rewards.create())

    val setupElement = document.getElementById("setup")
    if (setupElement != null) createRoot(setupElement).render(setup.create())
}