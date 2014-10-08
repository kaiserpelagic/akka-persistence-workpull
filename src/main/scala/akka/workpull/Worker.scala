package com.akka.workpull 

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Terminated
import org.slf4j.LoggerFactory
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.{ Failure, Success }
import PullingPattern._

abstract class Worker[T : ClassTag](val master: ActorRef)(implicit manifest: Manifest[T]) extends Actor {

  implicit val ec = context.dispatcher

  override def preStart {
    master ! RegisterWorker(self)
  } 

  def receive = idle

  def working(work: T): Receive = {
    case WorkAvailable => // I'll pass since I'm already working
     
    case Work(work: T) => // I'll pass since I'm already working
    
    case WorkFinished(work: T) => {
      context.become(idle) 
      master ! GimmeWork
    }
  }

  def idle: Receive = {
    case WorkAvailable => { 
      master ! GimmeWork
    }

    case Work(work: T) => {
      context.become(working(work))
      doWork(work) onComplete { 
        case Success(result) => {
          master ! WorkCompleted(work)
          self ! WorkFinished(work)
        }
        case Failure(ex) => { 
          log.error(ex.toString)
          master ! WorkFailed(work)
          self ! WorkFinished(work)
        }
      }
    }
  }

  def doWork(work: T): Future[_] 

  private val log = LoggerFactory.getLogger(getClass)
}
