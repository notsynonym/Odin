package com.odtheking.odin.features.impl.dungeon

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.TickTask
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils

object ScoreMessages : Module(
    name = "Score Messages",
    description = "Sends customizable party chat messages when dungeon score thresholds are reached."
) {
    private val score270Toggle by BooleanSetting("270 Message", true, desc = "Sends a party chat message when 270 score is reached.")
    private val score270Message by StringSetting("270 Text", "Score {score} reached!", 128, desc = "Message sent when 270 score is reached.").withDependency { score270Toggle }

    private val score300Toggle by BooleanSetting("300 Message", true, desc = "Sends a party chat message when 300 score is reached.")
    private val score300Message by StringSetting("300 Text", "300 score reached in {time}!", 128, desc = "Message sent when 300 score is reached.").withDependency { score300Toggle }

    private var lastScore: Int? = null
    private val sentScores = mutableSetOf<Int>()

    init {
        TickTask(10) {
            if (!enabled || !DungeonUtils.inDungeons) {
                lastScore = null
                sentScores.clear()
                return@TickTask
            }

            val currentScore = DungeonUtils.score
            val previousScore = lastScore
            lastScore = currentScore

            if (previousScore == null) {
                sentScores.addAll(thresholds().filter { currentScore >= it.score }.map { it.score })
                return@TickTask
            }

            thresholds()
                .filter { it.score !in sentScores && previousScore < it.score && currentScore >= it.score }
                .forEach {
                    sentScores.add(it.score)
                    sendPartyMessage(it.message, currentScore, it.score)
                }
        }

        on<WorldEvent.Load> {
            lastScore = null
            sentScores.clear()
        }
    }

    private fun thresholds(): List<ScoreMessage> =
        listOfNotNull(
            if (score270Toggle) ScoreMessage(270, score270Message) else null,
            if (score300Toggle) ScoreMessage(300, score300Message) else null
        )

    private fun sendPartyMessage(message: String, score: Int, threshold: Int) {
        val formatted = message
            .replace("{score}", score.toString())
            .replace("{threshold}", threshold.toString())
            .replace("{time}", DungeonUtils.dungeonTime)
            .replace("{floor}", DungeonUtils.floor?.name ?: "Unknown")
            .trim()

        if (formatted.isNotEmpty()) sendCommand("pc $formatted")
    }

    private data class ScoreMessage(val score: Int, val message: String)
}
