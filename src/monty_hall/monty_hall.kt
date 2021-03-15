@file:JvmName("MontyHall")

package monty_hall

import javafx.scene.image.Image
import tornadofx.*
import java.io.FileInputStream

fun main(args: Array<String>) {
    if (args.isNotEmpty() && args[0] == "gui") {
        launch<MontyHallApp>()
    } else {
        print("Input number of runs [20000]: ")
        val input = readLine()
        val numRuns = if (input.isNullOrBlank()) 20000 else input.toInt()

        var firstChoiceWins = 0
        var switchWins = 0

        val doors = listOf(1, 2, 3)

        for (i in 1..numRuns) {
            val winner = doors.random()
            val firstChoice = doors.random()
            if (firstChoice == winner) {
                firstChoiceWins += 1
            } else {
                switchWins += 1
            }
        }

        println("Wins with original pick = $firstChoiceWins")
        println("Wins with changed pick = $switchWins")
        println("Probability of winning with initial guess: ${firstChoiceWins / numRuns.toDouble()}")
        println("Probability of winning by switching: ${switchWins / numRuns.toDouble()}")
    }
}

class MontyHallApp : App(MontyHallCanvas::class)

class MontyHallCanvas : View("Milky Way Galaxy") {
    override val root = gridpane {
        hgap = 10.0
        vgap = 10.0

        row {
            imageview(image = Image(FileInputStream("src/monty_hall/all_closed.png"))) {
                gridpaneConstraints {
                    columnSpan = 3
                }
            }
        }
        row {
            text("Behind one door is CASH!")
            text("Pick a door:")
            text("unchanged wins = 0")
        }
    }
}
