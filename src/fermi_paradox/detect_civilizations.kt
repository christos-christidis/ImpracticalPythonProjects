package fermi_paradox

import javafx.scene.chart.NumberAxis
import javafx.scene.paint.Color
import org.apache.commons.math.analysis.polynomials.PolynomialFunction
import org.apache.commons.math.optimization.fitting.PolynomialFitter
import org.apache.commons.math.optimization.general.GaussNewtonOptimizer
import tornadofx.*
import java.io.File
import kotlin.math.*
import kotlin.random.Random

private const val NUM_EQUIV_VOLUMES = 1000
private const val MAX_CIVS = 5000
private const val NUM_TRIALS = 1000
private const val CIV_STEP_SIZE = 100

private const val SCALE = 225           // The Earth's radio bubble = 225LY
private const val NUM_CIVS = 15600000   // A number picked for the visualization

// actual Milky Way dimensions (in LY)
private const val DISC_RADIUS = 50000
private const val DISC_HEIGHT = 1000
private const val DISC_VOLUME = Math.PI * DISC_RADIUS * DISC_RADIUS * DISC_HEIGHT

private const val STAR_HAZE_DENSITY = 4

fun main() {
    val file = File("src/fermi_paradox/coefficients.txt")

    if (!file.exists()) {
        val (x, y) = computeProbabilities()
        val polynomialValues = fitPolynomialToData(x, y, file)

        launch<ProbabilityApp>(
            arrayOf(
                x.joinToString(" "),
                y.joinToString(" "),
                polynomialValues.joinToString(" ")
            )
        )
    } else {
        val (discRadiusScaled, discVolumeScaled) = scaleGalaxy()
        val detectProbability = detectProbability(discVolumeScaled)
        launch<GalaxyApp>(
            arrayOf(discRadiusScaled.toString(), detectProbability.toString())
        )
    }
}

// for different total number of civs in the galaxy, we compute the probability p that our civ is one of those civs that
// are alone in their own area and so cannot detect other civs. Then we take 1-p which is the probability that we'd detect
// one or more civs out there!
private fun computeProbabilities(): Pair<List<Double>, List<Double>> {
    println("Calculating the probabilities...")

    val x = mutableListOf<Double>()
    val y = mutableListOf<Double>()

    for (numCivs in 2..MAX_CIVS + 1 step CIV_STEP_SIZE) {
        var numSingleCivs = 0
        for (i in 0 until NUM_TRIALS) {
            val locations = mutableListOf<Int>()
            while (locations.size < numCivs) {
                locations.add(Random.nextInt(1, NUM_EQUIV_VOLUMES + 1))
            }

            numSingleCivs += locations.groupingBy { it }.eachCount().count { it.value == 1 }
        }

        val civsPerVolume = numCivs / NUM_EQUIV_VOLUMES.toDouble()
        x.add(civsPerVolume)

        val prob = 1 - numSingleCivs / (numCivs * NUM_TRIALS).toDouble()
        y.add(prob)
    }

    return Pair(x, y)
}

// we fit a polynomial function to our data so that we don't have to compute this data again and again
private fun fitPolynomialToData(x: List<Double>, y: List<Double>, coeffsFile: File): List<Double> {
    println("Fitting a polynomial to the data...")

    val fitter = PolynomialFitter(4, GaussNewtonOptimizer(true))
    for (i in x.indices) {
        fitter.addObservedPoint(1.0, x[i], y[i])
    }
    val polynomialFunction = fitter.fit()

    coeffsFile.writeText(polynomialFunction.coefficients.joinToString(" "))

    return x.map { polynomialFunction.value(it) }
}

private fun scaleGalaxy(): Pair<Int, Double> {
    val discRadiusScaled = (DISC_RADIUS / SCALE.toDouble()).roundToInt()
    val bubbleVolume = 4 / 3.0 * Math.PI * (SCALE / 2.0).pow(3.0)
    val discVolumeScaled = DISC_VOLUME / bubbleVolume
    return Pair(discRadiusScaled, discVolumeScaled)
}

private fun detectProbability(discVolumeScaled: Double): Double {
    val ratio = NUM_CIVS / discVolumeScaled
    return when {
        ratio < 0.002 -> 0.0
        ratio >= 5 -> 1.0
        else -> {
            val coefficients = File("src/fermi_paradox/coefficients.txt").readText().split(" ")
                .map { it.toDouble() }.toDoubleArray()
            val value = PolynomialFunction(coefficients).value(ratio)
            (value * 1000).roundToInt() / 1000.0    // rounds to 3 decimals
        }
    }
}


// This shows a chart that compares the data we calculated for probability of detection to the polynomial function that
// we fit to that data. Should be nearly identical
private class ProbabilityApp : App(ProbabilityChart::class)

private class ProbabilityChart : View() {
    private val x = getData(0)
    private val y = getData(1)
    private val fit = getData(2)

    override val root = linechart("Probability of detection", NumberAxis(), NumberAxis()) {
        multiseries("data", "fit") {
            for (i in x.indices) {
                data(x[i], y[i], fit[i])
            }
        }
    }

    private fun getData(paramNumber: Int): List<Double> {
        return app.parameters.unnamed[paramNumber].split(" ").map { it.toDouble() }
    }
}


// This is a drawing of the galaxy!
class GalaxyApp : App(GalaxyCanvas::class)

class GalaxyCanvas : View("Milky Way Galaxy") {
    override val root = stackpane {
        group {
            rectangle(0, 0, 1000, 800) {
                fill = Color.BLACK
            }

            val discRadiusScaled = app.parameters.unnamed[0].toInt()
            val detectProbability = app.parameters.unnamed[1].toDouble()

            val spiralStars = mutableListOf<Triple<Double, Double, Int>>()
            createSpiralStars(-0.3, discRadiusScaled, 2.0, 1.5, 0, spiralStars)
            createSpiralStars(-0.3, discRadiusScaled, 1.91, 1.5, 1, spiralStars)
            createSpiralStars(-0.3, -discRadiusScaled, 2.0, 1.5, 0, spiralStars)
            createSpiralStars(-0.3, -discRadiusScaled, -2.09, 1.5, 1, spiralStars)
            createSpiralStars(-0.3, -discRadiusScaled, 0.5, 1.5, 0, spiralStars)
            createSpiralStars(-0.3, -discRadiusScaled, 0.4, 1.5, 1, spiralStars)
            createSpiralStars(-0.3, -discRadiusScaled, -0.5, 1.5, 0, spiralStars)
            createSpiralStars(-0.3, -discRadiusScaled, -0.6, 1.5, 1, spiralStars)

            for ((x, y, arm) in spiralStars) {
                if (arm == 0 && x.toInt() % 2 == 0) {
                    circle(centerX = x, centerY = y, radius = 1) {
                        translateX = 500.0
                        translateY = 400.0
                        fill = Color.WHITE
                    }
                } else if (arm == 0 && x.toInt() % 2 == 1) {
                    circle(centerX = x, centerY = y, radius = 0.75) {
                        translateX = 500.0
                        translateY = 400.0
                        fill = Color.WHITE
                    }
                } else if (arm == 1) {
                    circle(centerX = x, centerY = y, radius = 0.5) {
                        translateX = 500.0
                        translateY = 400.0
                        fill = Color.WHITE
                    }
                }
            }

            for ((x, y) in starHaze(discRadiusScaled)) {
                circle(centerX = x, centerY = y, radius = 0.5) {
                    translateX = 500.0
                    translateY = 400.0
                    fill = Color.WHITE
                }
            }

            textflow {
                translateX = 20.0
                translateY = 20.0
                style = "-fx-background-color: black;"
                text("One pixel = $SCALE LY") {
                    fill = Color.WHITE
                }
            }
            textflow {
                translateX = 20.0
                translateY = 50.0
                style = "-fx-background-color: black;"
                text("Radio Bubble Diameter = $SCALE LY") {
                    fill = Color.WHITE
                }
            }
            textflow {
                translateX = 20.0
                translateY = 80.0
                style = "-fx-background-color: black;"
                text("Probability of detection for $NUM_CIVS civilizations = $detectProbability") {
                    fill = Color.WHITE
                }
            }
        }
    }
}

// in the book, it explains the 'spiral' equation
private fun createSpiralStars(
    b: Double, r: Int, rotationFactor: Double, fuzzFactor: Double, arm: Int,
    spiralStars: MutableList<Triple<Double, Double, Int>>
) {
    val fuzz = (0.03 * abs(r)).toInt()
    val thetaMaxDegrees = 520
    for (i in 0 until thetaMaxDegrees) {
        val theta = Math.toRadians(i.toDouble())
        val x = r * exp(b * theta) * cos(theta + Math.PI * rotationFactor) +
                Random.nextInt(-fuzz, fuzz) * fuzzFactor
        val y = r * exp(b * theta) * sin(theta + Math.PI * rotationFactor) +
                Random.nextInt(-fuzz, fuzz) * fuzzFactor

        spiralStars.add(Triple(x, y, arm))
    }
}

private fun starHaze(discRadiusScaled: Int): List<Pair<Int, Int>> {
    val stars = mutableListOf<Pair<Int, Int>>()

    for (i in 0 until discRadiusScaled * STAR_HAZE_DENSITY) {
        stars.add(randomPolarCoordinates(discRadiusScaled))
    }
    return stars
}

private fun randomPolarCoordinates(discRadiusScaled: Int): Pair<Int, Int> {
    val r = Random.nextDouble(0.0, 1.0)
    val theta = Random.nextDouble(0.0, 2 * Math.PI)
    val x = (sqrt(r) * cos(theta) * discRadiusScaled).roundToInt()
    val y = (sqrt(r) * sin(theta) * discRadiusScaled).roundToInt()
    return Pair(x, y)
}
