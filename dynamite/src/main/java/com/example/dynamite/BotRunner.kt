package com.example.dynamite

import com.softwire.dynamite.game.Move
import com.softwire.dynamite.runner.*
import java.util.HashMap
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@Serializable
data class SerializeMarkov(val hash: HashMap<TwoMoveState, HashMap<Move, Entry>>)

object BotRunner {
    @ImplicitReflectionSerializer
    @JvmStatic
    fun main(args: Array<String>) {
        var bot = MyBot()
        val results: Results = DynamiteRunner.playGames({ bot })

        for (( i,v ) in bot.markovProbabilities) {
//            println("$i : $v")
        }

        var json = Json( JsonConfiguration.Stable)

        print(json.toJson(

            SerializeMarkov(bot.markovProbabilities)
        ))


    }
}