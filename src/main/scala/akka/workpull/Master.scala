package com.akka.workpull 

import akka.actor._
import akka.persistence._
import org.slf4j.LoggerFactory
import PullingPattern._
import WorkState._

class Master[T <: Queueable] extends PersistentActor {

  val persistenceId = "workpoolMaster"
  
  private val workers = scala.collection.mutable.Map.empty[ActorRef, Option[(ActorRef, T)]]

  // event sourced
  private var state = WorkState.empty[T]

  private def notifyWorkers(): Unit = {
    if (state.hasWork) {
      workers.foreach { 
        case (worker, work) if work.isEmpty => worker ! WorkAvailable 
        case _ =>
      }
    }
  }
  
  val receiveRecover: Receive = {
    case event: WorkEvent => state = state.update(event) 
  } 

  val receiveCommand: Receive = {

    case work: Work[T] => {
      persist(WorkAddedEvt(work.work)) { event => 
        state = state.update(event) 
        notifyWorkers()
      }
    }

    case GimmeWork => {
      if (state.hasWork) {
        val work = state.pop 
        persist(WorkStartedEvt(work)) { event =>
          state = state.update(event) 
          workers += (sender -> Some(sender -> work))
          sender ! Work(work)
        }
      }
    }

    case completed: WorkCompleted[T] => {
      log.info(s"work completed $completed")
      workers += (sender -> None)
      persist(WorkCompletedEvt(completed.work)) { event =>
        state = state.update(event)
      }
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

    case CancelJob(id) => {
      persist(WorkCancelEvt(id)) { event =>
        state = state.update(event)
      }
      sender ! true
    }
  }

  private val log = LoggerFactory.getLogger(getClass)
}

