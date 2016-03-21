package com.sample.lambda

import java.io.File
import java.nio.file.{Files, StandardCopyOption}

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{CannedAccessControlList, PutObjectRequest}
import org.im4java.core.{ConvertCmd, IMOperation}

class S3 {
  def handler(event: S3Event, context: Context): String = {
    val lambdaLogger = context.getLogger
    val record = event.getRecords.get(0)

    val bucket = record.getS3.getBucket.getName
    val key = record.getS3.getObject.getKey

    val pattern = """(.+)\.(.+)""".r
    val patternThumbnail = """(.+)_thumbnail\.(.+)""".r

    key match {
      case patternThumbnail(name, format) => {

      }
      case pattern(name, format) => {
        val client = new AmazonS3Client()
        val s3Object = client.getObject(bucket, key)

        val in = s3Object.getObjectContent
        val tmp = File.createTempFile("s3test", "input")
        Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING)

        val resizedImage = File.createTempFile("s3test", "output")

        val op: IMOperation = new IMOperation()
        op.addImage(tmp.getAbsolutePath)
        op.resize(100, 100)
        op.addImage(resizedImage.getAbsolutePath)

        val converter = new ConvertCmd()
        converter.run(op)

        val resizedKey = name + "_thumbnail." + format

        val putObjectRequest: PutObjectRequest = new PutObjectRequest(bucket, resizedKey, resizedImage)
        putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead)

        client.putObject(putObjectRequest)
      }
    }

    key
  }
}
