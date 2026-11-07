package com.nexus.ai.editor

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

class FFmpegHelper {
    fun combineVideoWithAudio(videoUrl: String, audioUrl: String, context: Context, onResult: (String?) -> Unit) {
        val outputFile = File(context.cacheDir, "montage_${System.currentTimeMillis()}.mp4")
        val command = "-y -i $videoUrl -i $audioUrl -t 15 -c:v libx264 -c:a aac -shortest ${outputFile.absolutePath}"
        FFmpegKit.executeAsync(command) { session ->
            if (ReturnCode.isSuccess(session.returnCode)) onResult(outputFile.absolutePath) else onResult(null)
        }
    }

    fun createLongMontage(videoUrl: String, audioUrl: String, clips: List<Pair<Int, Int>>, context: android.content.Context, onResult: (String?) -> Unit) {
        if (clips.isEmpty()) { onResult(null); return }
        val outputFile = java.io.File(context.cacheDir, "long_montage_${System.currentTimeMillis()}.mp4")
        val filterBuilder = StringBuilder()
        val inputs = mutableListOf<String>()

        clips.forEachIndexed { index, (start, end) ->
            inputs.add("-ss $start -to $end -i $videoUrl")
            filterBuilder.append("[$index:v]trim=start=$start:end=$end,setpts=PTS-STARTPTS[v$index]; ")
        }
        val concatInputs = clips.indices.joinToString("") { "[v$it]" }
        filterBuilder.append("${concatInputs}concat=n=${clips.size}:v=1:a=0[v]")

        val command = "-y ${inputs.joinToString(" ")} -i $audioUrl -filter_complex \"$filterBuilder\" -map \"[v]\" -map ${clips.size}:a -shortest -c:v libx264 -c:a aac ${outputFile.absolutePath}"

        com.arthenica.ffmpegkit.FFmpegKit.executeAsync(command) { session ->
            if (com.arthenica.ffmpegkit.ReturnCode.isSuccess(session.returnCode)) onResult(outputFile.absolutePath)
            else onResult(null)
        }
    }
}
