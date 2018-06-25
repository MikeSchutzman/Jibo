package com.example.android.teammeetingjibo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.jibo.apptoolkit.protocol.CommandLibrary
import com.jibo.apptoolkit.protocol.OnConnectionListener
import com.jibo.apptoolkit.protocol.model.EventMessage
import com.jibo.apptoolkit.android.JiboRemoteControl
import com.jibo.apptoolkit.android.model.api.Robot
import java.io.InputStream
import android.widget.Toast
import com.example.android.teammeetingjibo.R.id.*
import com.jibo.apptoolkit.protocol.model.Command
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer


class MainActivity : AppCompatActivity(), OnConnectionListener, CommandLibrary.OnCommandResponseListener{

    // Variable for using the command library
    private var mCommandLibrary: CommandLibrary? = null

    // List of robots associated with a user's account
    private var mRobots: ArrayList<Robot>? = null

    private val coords: IntArray = intArrayOf(-1,-1,-1)

    private var p1Count = 102
    private var p2Count = 101
    private var p3Count = 1
    private var p4Count = 100
    private var p1Speech : MutableList<String> = ArrayList()
    private var p2Speech : MutableList<String> = ArrayList()
    private var p3Speech : MutableList<String> = ArrayList()
    private var p4Speech : MutableList<String> = ArrayList()
    private var lookingAround = false
    private var fixedRateTimerRandom = fixedRateTimer(name = "lookAround",
            initialDelay = 0, period = 500000000) {
    }

    // Authentication
    private val onAuthenticationListener = object : JiboRemoteControl.OnAuthenticationListener {

        override fun onSuccess(robots: ArrayList<Robot>) {

            // Add the list of user's robots to the robots array
            mRobots = ArrayList(robots)

            // Print a list of all robots associated with the account and their index in the array
            // so we can choose the one we want to connect to
            var i = 0
            var botList = ""
            while (i < mRobots!!.size) {
                botList += i.toString() + ": " + mRobots!!.get(i).robotName + "\n"
                i++
            }

            Toast.makeText(this@MainActivity, botList, Toast.LENGTH_SHORT).show()

            // Disable Log In and enable Connect and Log Out buttons when authenticated
            loginButton?.isEnabled = false
            connectButton?.isEnabled = true
            logoutButton?.isEnabled = true
        }

        // If there's an authentication error
        override fun onError(throwable: Throwable) {

            // Log the error to the app
            Toast.makeText(this@MainActivity, "API onError:" + throwable.localizedMessage, Toast.LENGTH_SHORT).show()
        }

        // If there's an authentication cancellation
        override fun onCancel() {

            // Log the cancellation to the app
            Toast.makeText(this@MainActivity, "Authentication canceled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Assign all buttons a function when clicked
        loginButton.setOnClickListener { onLoginClick() }
        connectButton.setOnClickListener { onConnectClick() }
        disconnectButton.setOnClickListener { onDisconnectClick() }
        logoutButton.setOnClickListener { onLogOutClick() }
        interactButton.setOnClickListener { onInteractClick() }
        listenButton.setOnClickListener { onListenClick() }
        moveButton.setOnClickListener { onMoveClick() }
        nodButton.setOnClickListener { onNodClick() }
        no.setOnClickListener { onNoClick() }
        rotate.setOnClickListener{onRotateClick()}
        p1.setOnClickListener{onP1Click()}
        p2.setOnClickListener{onP2Click()}
        p3.setOnClickListener{onP3Click()}
        p4.setOnClickListener{onP4Click()}
        stopLookingAround.setOnClickListener{onStopLookingAround()}

        // Start with only the Log In button enabled
        loginButton.isEnabled = true
        connectButton.isEnabled = false
        disconnectButton.isEnabled = false
        logoutButton.isEnabled = false
        interactButton.isEnabled = false
        listenButton.isEnabled = false
        moveButton.isEnabled = false
        rotate.isEnabled=false
        nodButton.isEnabled = false
        no.isEnabled = false
        p1.isEnabled=false
        p2.isEnabled=false
        p3.isEnabled=false
        p4.isEnabled=false
        stopLookingAround.isEnabled=false




    }
        // Our connectivity functions

    // function for logging information
    private fun log(msg: String) {
        Log.d("TeamMeeting", msg)
    }

    // Log In
    fun onLoginClick() {
        JiboRemoteControl.instance.signIn(this, onAuthenticationListener)
    }

    // Connect
    fun onConnectClick() {

        // Make sure there is at least one robot on the account
        if (mRobots?.size == 0) {
            Toast.makeText(this@MainActivity, "No robots on that account", Toast.LENGTH_SHORT).show()
        }
        // Connect to the first robot on the account.
        // To connect to a different robot, replace `0` in the code below with the index
        // printed on-screen next to the correct robot name
        else {
            var botNum = 0
            if (connectSwitch.isChecked)
                botNum = 1
            var myBot = mRobots!![botNum]
            JiboRemoteControl.instance.connect(myBot, this)
        }

        // Disable the connect button while we're connecting
        // to prevent double-clicking
        connectButton?.isEnabled = false
    }

    fun sayText(txt: String)
    {
        mCommandLibrary?.say(txt, this)
    }

    // Disconnect
    fun onDisconnectClick() {
        JiboRemoteControl.instance.disconnect()

        // Disable the disconnect button while disconnecting
        disconnectButton?.isEnabled = false
    }

    // Log out
    fun onLogOutClick() {
        JiboRemoteControl.instance.logOut()

        // Once we're logged out, only enable Log In button
        loginButton.isEnabled = true
        connectButton.isEnabled = false
        disconnectButton.isEnabled = false
        logoutButton.isEnabled = false
        interactButton.isEnabled = false
        listenButton.isEnabled = false
        moveButton.isEnabled = false
        rotate.isEnabled=false
        nodButton.isEnabled = false
        no.isEnabled = false
        p1.isEnabled=false
        p2.isEnabled=false
        p3.isEnabled=false
        p4.isEnabled=false
        stopLookingAround.isEnabled=false

        // Log that we've logged out to the app
        Toast.makeText(this@MainActivity, "Logged Out", Toast.LENGTH_SHORT).show()
    }

    // Interact Button
    fun onInteractClick() {
        if (mCommandLibrary != null) {
            /*
            var actions = arrayOf("affection", "confused", "embarrassed", "excited",
                    "frustrated", "happy", "headshake", "laughing", "nod", "proud",
                    "relieved", "sad", "scared", "worried")*/
            //var text = "Hello, this message should activate the happy action?"
            log("onInteractClick was successfully called")
            /*
            for (act in actions) {
                log("current act: $act")
                var text = "$act <anim cat='$act'/>"
                mCommandLibrary?.say(text, this)
                Thread.sleep(4000)
            }*/
            var text = "This is a headshake <anim cat='headshake'/>"
            mCommandLibrary?.say(text, this)
            Thread.sleep(3000)
            text = "And this is a nod. <anim cat='nod'/>"
            mCommandLibrary?.say(text, this)
        }
    }

    // Listen Button
    fun onListenClick() {
        if (mCommandLibrary != null) {
            mCommandLibrary?.listen(10L, 3600L, "en", this)
            log("onListenClick was successfully called")
        }
    }

    // Move Button
    fun onMoveClick() {
        if (mCommandLibrary != null) {
            //var target = Command.LookAtRequest.PositionTarget(intArrayOf(10, 1, 1))
            //var target = Command.LookAtRequest.AngleTarget(intArrayOf(3, 1))
            //mCommandLibrary?.lookAt(target, this)
            log("onMoveClick successfully called")
            var deltaX = 0
            var deltaY = 0
            var deltaZ = 0
            // 1, 1, 1
            // 2, -3, 1
            // -2, 1, 1
            // -2, -3, 1
            if (positionTextX.text.toString() != "")
                deltaX = Integer.parseInt(positionTextX.text.toString())
            if (positionTextY.text.toString() != "")
                deltaY = Integer.parseInt(positionTextY.text.toString())
            if (positionTextZ.text.toString() != "")
                deltaZ = Integer.parseInt(positionTextZ.text.toString())
            //var target = Command.LookAtRequest.AngleTarget(intArrayOf(deltaX, deltaY))
            var target = Command.LookAtRequest.PositionTarget(intArrayOf(deltaX, deltaY, deltaZ))
            mCommandLibrary?.lookAt(target, this)

        }
    }


    // Move Button
    /*fun onSayMsgClick() {
        if (mCommandLibrary != null) {
            /*//var target = Command.LookAtRequest.PositionTarget(intArrayOf(10, 1, 1))
            //var target = Command.LookAtRequest.AngleTarget(intArrayOf(3, 1))
            //mCommandLibrary?.lookAt(target, this)
            log("onSayMsgClick successfully called")
            var deltaX = 0
            var deltaY = 0
            var deltaZ = 0
            // 1, 1, 1
            // 2, -3, 1
            // -2, 1, 1
            // -2, -3, 1
            if (positionTextX.text.toString() != "")
                deltaX = Integer.parseInt(positionTextX.text.toString())
            if (positionTextY.text.toString() != "")
                deltaY = Integer.parseInt(positionTextY.text.toString())
            if (positionTextZ.text.toString() != "")
                deltaZ = Integer.parseInt(positionTextZ.text.toString())
            //var target = Command.LookAtRequest.AngleTarget(intArrayOf(deltaX, deltaY))
            var target = Command.LookAtRequest.PositionTarget(intArrayOf(deltaX, deltaY, deltaZ))*/

            //-----------------------------------------------------------
            /*val txt = findViewById(R.id.positionTextX) as EditText
            val msg = txt.text.toString()
            mCommandLibrary?.say(msg, this)
            //mCommandLibrary?.lookAt(target, this)*/
            //-----------------------------------------------------------
            /*val txt = findViewById(R.id.positionTextZ) as EditText
            val msg = txt.text.toString()
            var target = Command.LookAtRequest.PositionTarget(intArrayOf(1, 1, 10))
            if ((msg.toInt() > 0)) {    //add checking to ensure z value was entered
                target = Command.LookAtRequest.PositionTarget(intArrayOf(1, 1, -10))
            }
            mCommandLibrary?.lookAt(target, this)*/
            /*var target = Command.LookAtRequest.PositionTarget(intArrayOf(1, 1, -10))
            mCommandLibrary?.lookAt(target, this)
            target = Command.LookAtRequest.PositionTarget(intArrayOf(1, 1, 10))
            mCommandLibrary?.lookAt(target, this)*/
            //---------------------------------------------
           /* mCommandLibrary?.say("Say something", this)
            mCommandLibrary?.listen(5,5,"en_US", this)*/
            //vid.cancel()
            //log(vid.cancel())
        }
    }*/
    //Display item (text, image, etc) on Jibo's screen
    fun displayItem(view: Command.DisplayRequest.DisplayView, onCommandResponseListener: CommandLibrary.OnCommandResponseListener, actionTime: Long)
    {
        val item = this
        val fixedRateTimer = fixedRateTimer(name = "lookAround",
                initialDelay = 0, period = actionTime) {
            mCommandLibrary?.display(view, item)
        }
        val fixedRateTimer2 = fixedRateTimer(name = "stop",
                initialDelay = actionTime, period = 1) {
                fixedRateTimer.cancel()
                this.cancel()
                mCommandLibrary?.display(Command.DisplayRequest.EyeView("eye"), item)
        }
    }
    //move Jibo's face to a certain location
    fun move(target: Command.LookAtRequest.BaseLookAtTarget, onCommandResponseListener: CommandLibrary.OnCommandResponseListener )
    {
        mCommandLibrary?.lookAt(target, onCommandResponseListener)
    }
    // Nod Gesture
    fun nod()
    {
        if (mCommandLibrary != null) {
            var test = Command.DisplayRequest.TextView("test", "I'm nodding!")//("test.JPG", Command.DisplayRequest.ImageData())
            //displayItem(test,this)
            displayItem(test,this, 4500)
            log("onNodClick successfully called")
            // val x = fCoords
            //val coordsTest: IntArray = intArrayOf((fCoords[0]).toInt(),(fCoords[1]).toInt(),(fCoords[2]).toInt())
            //log("Coordinates: ${fCoords[0]}")
            var deltaX = 0
            var deltaY = 0
            var deltaZ = 0
            if (positionTextX.text.toString() != "")
                deltaX = Integer.parseInt(positionTextX.text.toString())
            if (positionTextY.text.toString() != "")
                deltaY = Integer.parseInt(positionTextY.text.toString())
            if (positionTextZ.text.toString() != "")
                deltaZ = Integer.parseInt(positionTextZ.text.toString())

            val coords: IntArray = intArrayOf(deltaX,deltaY, deltaZ)
            val zCoord = coords[2]
            log("Z: $zCoord")
            coords[2] = 1
            var target = Command.LookAtRequest.PositionTarget(coords)
            move(target, this)
            if (Math.abs(zCoord-1) == 1)
                Thread.sleep(1200)
            else
                Thread.sleep((650*(Math.abs(zCoord-1))).toLong())
            coords[2]=0
            target = Command.LookAtRequest.PositionTarget(coords)
            move(target, this)
            coords[2]=1
            Thread.sleep(400)
            target = Command.LookAtRequest.PositionTarget(coords)
            move(target, this)
        }
    }
    //Jibo nods twice like a person would
    fun onNodClick() {
        nod()
        Thread.sleep(1200)
        nod()
    }
    //no gesture
    fun shakeHeadNo(){
        var target = Command.LookAtRequest.AngleTarget(intArrayOf(1, 0))
        move(target, this)
        Thread.sleep(400)
        target = Command.LookAtRequest.AngleTarget(intArrayOf(-2, 0))
        move(target, this)
        Thread.sleep(1000)
        target = Command.LookAtRequest.AngleTarget(intArrayOf(1, 0))
        move(target, this)
        Thread.sleep(1100)
        target = Command.LookAtRequest.AngleTarget(intArrayOf(0, 0))
        move(target, this)
    }

    fun onNoClick() {
        if (mCommandLibrary != null) {
            log("onNoClick successfullly called")
            shakeHeadNo()
            //Thread.sleep(1000)
            //shakeHeadNo()
        }
    }

    //testing rotate with angle
    fun onRotateClick()
    {
        if (mCommandLibrary != null) {
            var deltaX = 0
            var deltaY = 0
            var deltaZ = 0
            if (positionTextX.text.toString() != "")
                deltaX = Integer.parseInt(positionTextX.text.toString())
            if (positionTextY.text.toString() != "")
                deltaY = Integer.parseInt(positionTextY.text.toString())
            if (positionTextZ.text.toString() != "")
                deltaZ = Integer.parseInt(positionTextZ.text.toString())
            //var target = Command.LookAtRequest.AngleTarget(intArrayOf(deltaX, deltaY))
            var target = Command.LookAtRequest.CameraTarget(intArrayOf(deltaX,deltaY,deltaZ))
            move(target, this)
        }
    }
    //look at participant 1
    fun onP1Click()
    {
        if (mCommandLibrary != null) {
            var target = Command.LookAtRequest.PositionTarget(intArrayOf(-4,2,1))
            move(target, this)
            var test = Command.DisplayRequest.TextView("test", "I'm looking at Michael!")
            displayItem(test,this, 4000)
        }
    }
    //look at participant 2
    fun onP2Click()
    {
        if (mCommandLibrary != null) {
            var target = Command.LookAtRequest.PositionTarget(intArrayOf(-2,-2,1))
            move(target, this)
            var test = Command.DisplayRequest.TextView("test", "I'm looking at Ling!")
            displayItem(test,this, 4000)
        }
    }
    //look at participant 3
    fun onP3Click()
    {
        if (mCommandLibrary != null) {
            var target = Command.LookAtRequest.PositionTarget(intArrayOf(2,-2,1))
            move(target, this)
            var test = Command.DisplayRequest.TextView("test", "I'm looking at Sarah!")
            displayItem(test,this, 4000)
        }
    }
    //look at participant 4
    fun onP4Click()
    {
        if (mCommandLibrary != null) {
            var target = Command.LookAtRequest.PositionTarget(intArrayOf(3,0,1))
            move(target, this)
            var test = Command.DisplayRequest.TextView("test", "I'm looking at Nick!")
            displayItem(test,this, 4000)
        }
    }
    //look at a variable participant
    fun lookAt(participant: Int)
    {
        log("participant: $participant")
        var coordinates: IntArray
        var name: String
        if (participant==0)
        {
            coordinates = intArrayOf(-4,2,1)
            name = "Michael"
        }
        else if (participant==1)
        {
            coordinates = intArrayOf(-2,-2,1)
            name = "Ling"
        }
        else if (participant==2)
        {
            coordinates = intArrayOf(2,-2,1)
            name = "Sarah"
        }
        else
        {
            coordinates = intArrayOf(3,0,1)
            name = "Nick"
        }
        if (mCommandLibrary != null) {
            var target = Command.LookAtRequest.PositionTarget(coordinates)
            move(target, this)
            var test = Command.DisplayRequest.TextView("test", "I'm looking at $name!")
            displayItem(test,this, 4000)
        }
    }
    //have Jibo stop looking around randomly at participants who haven't spoken as much
    fun onStopLookingAround()
    {
        if (lookingAround) {
            fixedRateTimerRandom.cancel()
        }
        else {
            chooseWhoToLookAt()
        }
        lookingAround=!lookingAround
    }

    /*fun onvideoClick() {
        if (mCommandLibrary != null) {
            if (mCommandLibrary != null) {
                mCommandLibrary?.video(Command.VideoRequest.VideoType.Normal,10, this)
                log("onVideoClick was successfully called")
            }
        }
    }*/

    //have Jibo look at participants, with those who have spoken the least being Jibo's primary focus
    fun chooseWhoToLookAt()
    {
        var deltaX = 0
        var deltaY = 0
        var deltaZ = 0
        if (positionTextX.text.toString() != "")
            deltaX = Integer.parseInt(positionTextX.text.toString())
        if (positionTextY.text.toString() != "")
            deltaY = Integer.parseInt(positionTextY.text.toString())
        if (positionTextZ.text.toString() != "")
            deltaZ = Integer.parseInt(positionTextZ.text.toString())
        p1Count = deltaX
        p2Count = deltaY
        p3Count = deltaZ
        p4Count = 50
        fixedRateTimerRandom = fixedRateTimer(name = "lookAround",
                initialDelay = 0, period = 5000) {
            val p1Prob: Double
            val p2Prob: Double
            val p3Prob: Double
            val p4Prob: Double
            var sum = p1Count.toDouble() + p2Count + p3Count + p4Count
            if (sum ==0.0) {
                p1Prob = 0.25
                p2Prob = 0.25
                p3Prob = 0.25
                p4Prob = 0.25
            }
            else
            {
                p1Prob = Math.pow((p1Count / sum),-2.0)
                p2Prob = Math.pow((p2Count / sum),-2.0)
                p3Prob = Math.pow((p3Count / sum),-2.0)
                p4Prob = Math.pow((p4Count / sum),-2.0)
            }

            val probabilities : DoubleArray = doubleArrayOf(p1Prob,p2Prob,p3Prob,p4Prob)
            Arrays.sort(probabilities)
            val counts : MutableList<Int> = ArrayList()
            counts.add(p1Count)
            counts.add(p2Count)
            counts.add(p3Count)
            counts.add(p4Count)
            var low = p1Count
            var index=0
            for (i in counts)
                log("Counts orig: $i")
            for (i in counts.indices)
                if (counts.get(i)<low) {
                    low = counts.get(i)
                    index = i
                }
            counts.removeAt(index)
            for (i in counts)
                log("Counts removed low: $i")
            var medLow = counts.get(0)
            index=0
            for (i in counts.indices)
                if (counts.get(i)<medLow) {
                    medLow = counts.get(i)
                    index = i
                }
            counts.removeAt(index)
            for (i in counts)
                log("Counts removed medLow: $i")
            var medHigh = counts.get(0)
            index = 0
            if (counts.get(1)<medHigh)
            {
                index = 1
                medHigh=counts.get(1)
            }
            counts.removeAt(index)
            for (i in counts)
                log("Counts removed medHigh: $i")
            var high = counts.get(0)
            log("Counts high: $high")
            for (i in probabilities.indices) {
                if (i>0)
                    probabilities[i]+=probabilities[i-1]
            }

            var num = (Math.random()*probabilities[probabilities.lastIndex]+1)
            var people : MutableList<Int> = ArrayList()
            people.add(p1Count)
            people.add(p2Count)
            people.add(p3Count)
            people.add(p4Count)
            var temp : MutableList<Int> = ArrayList()
            for (i in people.indices)
                if(people[i] == high)
                    temp.add(i)
            for (i in temp)
                log("temp: $i")
            var highIndex = chooseRandomElement(temp)
            temp = ArrayList()
            for (i in people.indices)
                if(people[i] == medHigh && i!=highIndex)
                    temp.add(i)
            for (i in temp)
                log("temp: $i")
            var medHighIndex = chooseRandomElement(temp)
            temp = ArrayList()
            for (i in people.indices)
                if(people[i] == medLow && i!=highIndex && i!=medHighIndex)
                    temp.add(i)
            for (i in temp)
                log("temp: $i")
            var medLowIndex = chooseRandomElement(temp)
            var lowIndex = 0
            for (i in people.indices)
                if (i!=highIndex && i!=medHighIndex && i!=medLowIndex)
                    lowIndex = i
            log("Random number is: $num")
            if (num < probabilities[0]) {
                lookAt(highIndex)
            }
            else if (num < probabilities[1]) {
                lookAt(medHighIndex)
            }
            else if (num < probabilities[2]) {
                lookAt(medLowIndex)
            }
            else{
                lookAt(lowIndex)
            }
            log("prob 0 ${probabilities[0]}")
            log("prob 1 ${probabilities[1]}")
            log("prob 2 ${probabilities[2]}")
            log("prob 3 ${probabilities[3]}")
            log("low $low")
            log("medlow $medLow")
            log("medHigh $medHigh")
            log("high $high")
            log("low index $lowIndex")
            log("medLow index $medLowIndex")
            log("medHigh index $medHighIndex")
            log("high index $highIndex")
        }
    }
    //if multiple participants have spoken the same number of times, randomly chooses a participant to look at.
    fun chooseRandomElement(temp : MutableList<Int>): Int
    {
        var random = (Math.random()*temp.size).toInt()
        log("${temp.size} is the size!")
        return temp.get(random)
    }

    override fun onConnected() {}

    override fun onSessionStarted(commandLibrary: CommandLibrary) {
        mCommandLibrary = commandLibrary
        runOnUiThread {
            // Once we're connected and ready for commands,
            // enable the Disconnect and Say buttons
            disconnectButton?.isEnabled = true
            interactButton?.isEnabled = true
            listenButton?.isEnabled = true
            moveButton?.isEnabled = true
            nodButton?.isEnabled = true
            no.isEnabled = true
            rotate.isEnabled=true
            rotate.isEnabled=true
            p1.isEnabled=true
            p2.isEnabled=true
            p3.isEnabled=true
            p4.isEnabled=true
            stopLookingAround.isEnabled=true
            // Log that we're connected to the app
            Toast.makeText(this@MainActivity, "Connected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionFailed(throwable: Throwable) {
        runOnUiThread {
            // If connection fails, re-enable the Connect button so we can try again
            connectButton?.isEnabled = true

            // Log the error to the app
            Toast.makeText(this@MainActivity, "Connection failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDisconnected(i: Int) {
        runOnUiThread {
            // Re-enable Connnect & Say when we're disconnected
            loginButton.isEnabled = true
            connectButton.isEnabled = true
            disconnectButton.isEnabled = false
            logoutButton.isEnabled = false
            interactButton.isEnabled = false
            listenButton.isEnabled = false
            moveButton.isEnabled = false
            rotate.isEnabled=false
            nodButton.isEnabled = false
            no.isEnabled = false
            p1.isEnabled=false
            p2.isEnabled=false
            p3.isEnabled=false
            p4.isEnabled=false
            stopLookingAround.isEnabled=false

            // Log that we've disconnected from the app
            Toast.makeText(this@MainActivity, "Disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    // onCommandResponseListener overrides

    override fun onSuccess(s: String) {
        runOnUiThread { }
    }

    override fun onError(s: String, s1: String) {
        runOnUiThread {
            // Log the error to the app
            Toast.makeText(this@MainActivity, "error : $s $s1", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onEventError(s: String, errorData: EventMessage.ErrorEvent.ErrorData) {

        runOnUiThread {
            // Log the error to the app
            Toast.makeText(this@MainActivity, "error : " + s + " " + errorData.errorString, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSocketError() {
        runOnUiThread {
            // Log the error to the app
            Toast.makeText(this@MainActivity, "socket error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onEvent(s: String, baseEvent: EventMessage.BaseEvent) {
        log("String: $s, BaseEvent: $baseEvent")
        if(baseEvent.event.toString().equals("LookAtAchieved"))
        {
            val laa = baseEvent//EventMessage.LookAtAchievedEvent()
            val test = laa as EventMessage.LookAtAchievedEvent
            val testArray: IntArray = test.positionTarget
            coords[0] = testArray[0]
            coords[1] = testArray[1]
            coords[2] = testArray[2]
        }
    }

    override fun onPhoto(s: String, takePhotoEvent: EventMessage.TakePhotoEvent, inputStream: InputStream) {}

    override fun onVideo(s: String, videoReadyEvent: EventMessage.VideoReadyEvent, inputStream: InputStream) {
        log("saw: $s, inputStream: $inputStream")
    }

    override fun onListen(transactID: String, speech: String) {
        log("Heard: $speech")
        var text = "$speech"
        if (text == ""){
            text = "Sorry, did you say something?"
            //onSayMsgClick()
            //return
        } else if (text.toLowerCase().contains("jibo")){
            text = "Hi! Did someone say Jibo? How can I help you?"
        } else if (text.toLowerCase().contains("happy")){
            text = "<anim cat='happy'/>"
        } else if (text.toLowerCase().contains("surprised")){
            text = "<anim cat='surprised' nonBlocking='true' endNeutral='true' />"
        } else if (text.toLowerCase().contains("hello")){
            text = "Hello to you too!"
        } else if (text.toLowerCase().contains(" hi ")){
            text = "Hi! How are you?"
        } else if(text.toLowerCase().contains("i'm going to fail")){

        } else if(text.toLowerCase().contains("what round")){

        } else if(text.toLowerCase().contains("don't have the right")){

        } else if(text.toLowerCase().contains("hmm")){

        } else if(text.toLowerCase().contains("robot")){
            text = "My name is Jibo!"
        } else if(text.toLowerCase().contains("yale")){

        } else if(text.toLowerCase().contains("mistakes")){

        } else if(text.toLowerCase().contains("nevermind")){

        } else if(text.toLowerCase().contains("i guess")){

        } else if (text.toLowerCase().contains("i think")){
            var pos = text.toLowerCase().indexOf("i think")
            var stmt = text.substring(pos+7)
            var num = (Math.random()*2).toInt()
            if (num == 0)
                text = "Do you all agree that $stmt?"
            else
                text = "So you are not sure that $stmt?"
        } else if (text.toLowerCase().contains(" hope ")){
            text = "What do you hope for?"
        }

        mCommandLibrary?.say(text, this)
        Thread.sleep(2000)
        onListenClick()
    }

    override fun onParseError() {}

}
