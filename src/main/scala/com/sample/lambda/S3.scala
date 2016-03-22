package com.sample.lambda

import java.io.File
import java.nio.file.{Files, StandardCopyOption}

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{CannedAccessControlList, ObjectMetadata, PutObjectRequest}
import com.typesafe.config.ConfigFactory
import org.im4java.core.{ConvertCmd, IMOperation}

class S3 {
  def handler(event: S3Event, context: Context): String = {
    val config = ConfigFactory.load()
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

        val inMetadata = s3Object.getObjectMetadata
        val input = s3Object.getObjectContent
        val inputFile = File.createTempFile("s3handler", "input")
        Files.copy(input, inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        val resizedImage = File.createTempFile("s3handler", "resizedImage")

        val op = new IMOperation()
        op.addImage(inputFile.getAbsolutePath)
        op.resize(config.getInt("resized.width"), config.getInt("resized.height"))
        op.addImage(resizedImage.getAbsolutePath)

        val converter = new ConvertCmd()
        converter.run(op)

        val resizedKey = name + "_thumbnail." + format

        val putObjectRequest = new PutObjectRequest(bucket, resizedKey, resizedImage)
        val resizedImageMetadata = new ObjectMetadata()
        resizedImageMetadata.setContentType(inMetadata.getContentType)
        putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead).withMetadata(resizedImageMetadata)

        client.putObject(putObjectRequest)
      }
    }

    key
  }
}
