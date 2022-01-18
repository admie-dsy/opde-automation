package com.ipto.opdefx.requests

import org.apache.commons.lang3.StringEscapeUtils
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.conn.ssl.{NoopHostnameVerifier, SSLContextBuilder, TrustAllStrategy}
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import zio._

import scala.util.Try

object RequestBroker {

  def getConnectionStatus(endpoint:String):ZIO[Any, Throwable, String] = {

    val client = ZIO.succeed(HttpClients
      .custom()
      .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
      .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
      .build())

    val send:HttpGet => Task[String] = post => ZIO.bracket(client)(client => ZIO.succeed(client.close())) { client =>
      ZIO.fromTry {
        Try {
          val resp = client.execute(post)
          val respEntity = resp.getEntity
          val respStr = EntityUtils.toString(respEntity)
          resp.close()
          StringEscapeUtils.unescapeXml(respStr)
        }
      }
    }

    for {
      get <- ZIO.succeed{new HttpGet(endpoint)}
      response <- send(get)
    } yield response

  }

  def sendRequest(endpoint:String, action:String, payload:ZIO[Any, Throwable, Array[Byte]]):ZIO[Any, Throwable, String] = {
    val client = ZIO.succeed(HttpClients
      .custom()
      .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
      .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
      .build())

    val request = for {
      p <- payload
      post <- ZIO.succeed{
        val postReq = new HttpPost(endpoint)
        postReq.setHeader("SOAPAction",  action)
        postReq.setHeader("content-type", "text/xml;charset=UTF-8")
        postReq.setHeader("Accept", "[*/*]")
        val entity = new ByteArrayEntity(p)
        postReq.setEntity(entity)
        postReq
      }
    } yield post

    val send:HttpPost => Task[String] = post => ZIO.bracket(client)(client => ZIO.succeed(client.close())) { client =>
      ZIO.fromTry {
        Try {
          val resp = client.execute(post)
          val respEntity = resp.getEntity
          val respStr = EntityUtils.toString(respEntity)
          resp.close()
          StringEscapeUtils.unescapeXml(respStr)
        }
      }
    }

    for {
      post <- request
      response <- send(post)
    } yield response
  }

  def sendRequest(endpoint:String, action:String, payload:Array[Byte]):ZIO[Any, Throwable, String] = {

    val client = ZIO.succeed(HttpClients
      .custom()
      .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
      .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
      .build())

    val request = ZIO.succeed{
      val post = new HttpPost(endpoint)
      post.setHeader("SOAPAction",  action)
      post.setHeader("content-type", "text/xml;charset=UTF-8")
      post.setHeader("Accept", "[*/*]")

      val entity = new ByteArrayEntity(payload)
      post.setEntity(entity)
      post
    }

    val send:HttpPost => Task[String] = post => ZIO.bracket(client)(client => ZIO.succeed(client.close())) { client =>
      ZIO.fromTry {
        Try {
          val resp = client.execute(post)
          val respEntity = resp.getEntity
          val respStr = EntityUtils.toString(respEntity)
          resp.close()
          StringEscapeUtils.unescapeXml(respStr)
        }
      }
    }

    for {
      post <- request
      response <- send(post)
    } yield response

  }

}
