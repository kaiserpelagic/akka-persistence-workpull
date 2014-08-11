package com.akka.workpull 

import akka.actor._
import akka.persistence._
import scala.collection.mutable.{ Map, Queue }
import org.slf4j.LoggerFactory
import PullingPattern._

class Master[T] extends PersistentActor {

  val persistenceId = "workpoolMaster"
  
  private val workers = Map.empty[ActorRef, Option[(ActorRef, T)]]

  private val queue = Queue.empty[T]

  private var work: Option[T] = None

  private def notifyWorkers(): Unit = {
    if (!queue.isEmpty) {
      workers.foreach { 
        case (worker, work) if work.isEmpty => worker ! WorkAvailable 
        case _ =>
      }
    }
  }
  
  def enqueue(enqueue: Enqueue[T]) = {
    log.info(s"ENQUEUE EVT $enqueue")
    queue += enqueue.work
  }
  
  def dequeue(ignore: Dequeue) = { 
    log.info(s"DEQUEUE EVT")
    if (!queue.isEmpty) work = Some(queue.dequeue)
    else work = None
  }  

  val receiveRecover: Receive = {
    case work: Enqueue[T] => enqueue(work)
    case Dequeue => dequeue(Dequeue)
  } 

  val receiveCommand: Receive = {

    case work: Work[T] => {
      persist(Enqueue(work.work))(enqueue)
      notifyWorkers()
    }

    case GimmeWork => {
      persist(Dequeue)(dequeue)
      work foreach { w => 
        workers += (sender -> Some(sender -> w))
        sender ! Work(w)
      }
    }

    case completed: WorkCompleted[T] => {
      log.info(s"work ($completed) completed")
      workers += (sender -> None)
    }

    case failed: WorkFailed[T] => {
      log.info(s"work ($failed) failed")
      workers += (sender -> None)
      // retry logic if you so desire
    }

    case RegisterWorker(worker) => {
      log.info(s"registered worker ($worker)")
      context.watch(worker)
      workers += (worker -> None)
      notifyWorkers()
    }

    case Terminated(worker) => {
      log.info(s"worker ($worker) died - removing from the set of workers")
      workers -= worker
    }
  }

  private val log = LoggerFactory.getLogger(getClass)
}

