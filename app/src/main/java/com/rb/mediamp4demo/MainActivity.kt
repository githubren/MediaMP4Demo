package com.rb.mediamp4demo

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.Exception
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn.setOnClickListener {
            extractVideo()
        }
    }

    /**
     * 分离视频源文件
     */
    private fun extractVideo(){
        val mediaExtractor = MediaExtractor()
        var mediaMuxer : MediaMuxer? = null
        //视频源文件  包含视频和音频
        val mp4SourceFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).path+"/Camera/VID_20201119_182408.mp4")
        try {
            //设置数据源
            mediaExtractor.setDataSource(mp4SourceFile.path)
            //轨道索引  一段原始视频文件最起码包含视频（没声音）和音频两个轨道
            var videoIndex = -1
            //轨道格式信息  包含MIME、LANGUAGE、SAMPLE_RATE、WIDTH、HEIGHT等信息
            var mediaFormat : MediaFormat? = null
            //遍历轨道索引  找到视频轨道
            for (i in 0 until mediaExtractor.trackCount){
                val format = mediaExtractor.getTrackFormat(i)
                if ((format.getString(MediaFormat.KEY_MIME)?:"").startsWith("video/")){
                    //记录轨道号
                    videoIndex = i
                    //记录轨道格式信息
                    mediaFormat = format
                    break
                }
            }
            //设置轨道索引（视频轨道 不包含音频）
            mediaExtractor.selectTrack(videoIndex)
            //视频输出文件  去掉音频的
            val mp4OutputFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).path+"/Camera/output_test.mp4")
            if (mp4OutputFile.exists()){
                mp4OutputFile.delete()
            }
            //封装成MP4文件的对象
            mediaMuxer = MediaMuxer(mp4OutputFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            //添加轨道格式信息 并返回轨道索引
            val trackIndex = mediaMuxer.addTrack(mediaFormat!!)
            //分配缓冲区 设置大小  用源文件mediaformat中包含的
            val byteBuffer = ByteBuffer.allocate(mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
            //获取缓存区信息对象
            val bufferInfo = MediaCodec.BufferInfo()
            mediaMuxer.start()
            //开始读取数据
            while (true){
                //将读取到的数据存到缓冲区
                val readSampleSize = mediaExtractor.readSampleData(byteBuffer,0)
                //当读取到的数据小于0时证明读取完毕 退出死循环
                if (readSampleSize < 0){
                    mediaExtractor.unselectTrack(videoIndex)
                    break
                }
                //配置buffer信息
                bufferInfo.apply { 
                    size = readSampleSize
                    flags = mediaExtractor.sampleFlags
                    offset = 0
                    presentationTimeUs = mediaExtractor.sampleTime
                }
                //开始写入
                mediaMuxer.writeSampleData(trackIndex,byteBuffer,bufferInfo)
                //读取下一帧
                mediaExtractor.advance()
            }
            Toast.makeText(this, "分离视频完成", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaMuxer?.stop()
            mediaMuxer?.release()
            mediaExtractor.release()
        }
    }
}
