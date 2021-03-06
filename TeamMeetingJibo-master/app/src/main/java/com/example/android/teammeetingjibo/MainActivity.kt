/*TO DO:
VERBAL ON/OFF SWITCH
PROBABILITY FOR NON-VERBAL BEHAVIOR CATEGORIES
INTEGRATE NICK'S CODE
*/

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
import kotlin.concurrent.fixedRateTimer
import android.content.Context
import kotlinx.android.synthetic.main.activity_main.*
import java.util.ArrayList

import android.os.AsyncTask
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

import org.json.simple.JSONObject
import org.json.simple.parser.*


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


    private var `in`: BufferedReader? = null
    private var out: PrintWriter? = null
    private var ipAndPort: EditText? = null
    private var serverAddress: String? = null
    private var displayText: TextView? = null
    private var text: String? = null
    private var inputMessage: EditText? = null
    private var msg: String? = null
    private var connection: TextView? = null
    private var socket: Socket? = null
    private var obj: JSONObject? = null

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


        ipAndPort = findViewById(R.id.ipandportInput)
        connection = findViewById(R.id.connectionStatus)

        displayText = findViewById(R.id.DisplayText)
        inputMessage = findViewById(R.id.inputMessage)

        connectToServerButton.setOnClickListener{ connectToServer() }
        changeTextButton.setOnClickListener{ changeDisplay() }
        sendButton.setOnClickListener{ sendMessage() }
        speakButton.setOnClickListener{ speak() }

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

    // onClick for connectButton, function to create socket on IP at Port
    @Throws(IOException::class)
    fun connectToServer() {
        var proper = true // was the IP and Port given in the proper format?
        var valid = false // is the IP a valid IP?
        var pt = ""
        var input = ipAndPort!!.text.toString()
        if (input.indexOf(":") == -1) {
            connection!!.text = "Improper IP:Port format.\nPlease make sure you include the colon!"
            proper = false
        } else {
            serverAddress = input.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            pt = input.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            connection!!.text = "Connecting..."
        }

        // if input IP and Port were in the proper format, check the validity of IP
        if (proper) {
            valid = this.validIP(serverAddress)
        }

        // error message to user
        if (proper && !valid) {
            connection!!.text = "Invalid IP"
        }

        // input IP was valid, attempt to establish connection
        if (valid) {
            var connectTask = ConnectTask()
            connectTask.execute(serverAddress, pt)
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(ipAndPort?.windowToken, 0)
        }
    }

    // function to check if IP is a valid IP
    // taken from the tutoring app
    fun validIP(ip: String?): Boolean {
        if (ip == null) return false
        var ip2: String = ip.toString()
        if (ip2.isEmpty()) return false
        ip2 = ip2.trim { it <= ' ' }
        if ((ip2.length < 6) and (ip2.length > 15)) return false

        try {
            val pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
            val matcher = pattern.matcher(ip2)
            return matcher.matches()
        } catch (ex: PatternSyntaxException) {
            return false
        }

    }

    /*
        onClick for changeTextButton, function to read received message and change the display
        expecting to receive JSONObjects
        known issue: sometimes TextView won't wrap text properly, but the information is unaffected
                     restarting app seems to fix this problem sometimes, not sure how to replicate
    */
    @Throws(IOException::class)
    fun changeDisplay() {
        val changeText = DisplayText()
        changeText.execute()

//        val parser = JSONParser()
//        try {
//            obj = parser.parse(text) as JSONObject
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//
//        val info = "PID: " + obj!!["pid"] + "\n" + "Transcript: " + obj!!["transcript"]
//        displayText!!.text = info
    }

    // onClick for sendButton, function to send messages to server
    @Throws(IOException::class)
    fun sendMessage() {
        msg = inputMessage!!.text.toString()
        val txt = SendMessage()
        txt.execute()
    }

    // onClick for speakButton, function to say what was received
    fun speak() {
        val speech = "Participant number " + obj!!["pid"] + "said this. " + obj!!["transcript"]
        val nouns = "These are the nouns I heard." + obj!!["nouns"]
        val verbs = "These are the verbs I heard." + obj!!["verbs"]
        mCommandLibrary?.say(speech + "<break size='1'/>" + nouns + "<break size='1'/>" + verbs, this)
    }

    /*
        Separate classes that extend AsyncTask were required for each of the functions because
        in Android, you are not allowed to work with any kind of network connections in the main
        thread. In this file, even the changing display and sending messages functions had to be on
        a separate thread because changing display first required reading the received message which
        depended on reading incoming messages from the socket, and sending messages to a socket
        obviously relies on the network connection.
     */

    // separate class to establish socket on a separate background thread
    inner class ConnectTask : AsyncTask<String, String, Void>() {

        override fun doInBackground(vararg message: String): Void? {
            val port = Integer.parseInt(message[1])
            try {
                socket = Socket(serverAddress, port)
                connection!!.text = "Connected!"
                Log.d("Test Client Connection", "Connection made!")
            } catch (s: SocketTimeoutException) {
                s.printStackTrace()
            } catch (io: IOException) {
                io.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // setting up the way to send and receive messages
            try {
                `in` = BufferedReader(
                        InputStreamReader(socket!!.getInputStream()))
                out = PrintWriter(socket!!.getOutputStream(), true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }
    }

    // separate class to read and store incoming messages on a separate background thread
    inner class DisplayText : AsyncTask<String, Void, Void>() {


        override fun doInBackground(vararg message: String): Void? {
//            try {
//                text = `in`!!.readLine()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }

            // keeps listening for messages
            // to fix: doesn't wait for the say function to finish before reading and executing the
            // next piece of transcript - ends up cutting himself off
            // shouldn't be a problem since expected usage isn't to repeat everything people say
            while (socket!!.isConnected) {
                text = `in`!!.readLine()

                if (text != null) {
                    val parser = JSONParser()
                    try {
                        obj = parser.parse(text) as JSONObject
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val speech = "Participant " + obj!!["pid"] + "said <break size='0.5'/>" + obj!!["transcript"]
                    val nouns = "These are the nouns <break size='0.5'/>" + obj!!["nouns"]
                    val verbs = "These are the verbs <break size='0.5'/>" + obj!!["verbs"]
                    mCommandLibrary?.say(speech + "<break size='0.5'/>" + nouns + "<break size='0.5'/>" + verbs, this@MainActivity)
                    Thread.sleep(7500)
                }
            }

            return null
        }
    }

    // separate class to send messages to the server
    inner class SendMessage : AsyncTask<String, Void, Void>() {

        override fun doInBackground(vararg message: String): Void? {
            out!!.println(msg)
            return null
        }
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
        text.replace("ha", "")
        text.replace(" i ", " you ")
        text.replace(" i've ", " you've ")
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
        } else if(text.toLowerCase().indexOf("i think") == 0){
            var restOfSentence = text.toLowerCase().substring(7)
            var responses = arrayOf("Yeah", "I like that", "Mhmm", "I agree")
            var num = (Math.random()*responses.size).toInt()
            text = responses[num] + restOfSentence
            if (restOfSentence.equals("")||restOfSentence.equals(" "))
                text = ""
        } else if(text.toLowerCase().contains("i feel like")){
            var restOfSentence = text.toLowerCase().substring(11)
            var responses = arrayOf("Yup", "Good idea", "Oh, I see", "Exactly.", "hmmm")
            var num = (Math.random()*responses.size).toInt()
            text = responses[num] + restOfSentence
            if (restOfSentence.equals("")||restOfSentence.equals(" "))
                text = ""
        } else if(text.toLowerCase().contains("i'm pretty sure")){
            var restOfSentence = text.toLowerCase().substring(15)
            var responses = arrayOf("mhmm", "Maybe", "It makes sense that", "uh huh")
            var num = (Math.random()*responses.size).toInt()
            text = responses[num] + restOfSentence
            if (restOfSentence.equals("")||restOfSentence.equals(" "))
                text = ""
        } else if(text.toLowerCase().contains("i don't know")){
            var restOfSentence = text.toLowerCase().substring(12)
            var responses = arrayOf("uhhh", "hmmm", "interesting")
            var num = (Math.random()*responses.size).toInt()
            text = responses[num] + restOfSentence
            if (restOfSentence.equals("")||restOfSentence.equals(" "))
                text = ""
        } else if(text.toLowerCase().contains("who")){
            var restOfSentence = text.toLowerCase().substring(3)
            var responses = arrayOf("I know who", "Uhhmmm", "Hmm", "I wonder who")
            var num = (Math.random()*responses.size).toInt()
            text = responses[num] + restOfSentence
            if (restOfSentence.equals("")||restOfSentence.equals(" "))
                text = ""
        } else if(text.toLowerCase().contains("robot")){
            var restOfSentence = text.toLowerCase().substring(5)
            var responses = arrayOf("Yeah", "Yup", "Umm")
            var num = (Math.random()*responses.size).toInt()
            text = responses[num]
            if (restOfSentence.equals("")||restOfSentence.equals(" "))
                text = ""
        } else if(text.toLowerCase().contains("i wonder if")){
            var restOfSentence = text.toLowerCase().substring(11)
            var responses = arrayOf("Hmm", "It'd be interesting if", "It's worth considering if", "Uhhh if")
            var num = (Math.random()*responses.size).toInt()
            text = responses[num] + restOfSentence
            if (restOfSentence.equals("")||restOfSentence.equals(" "))
                text = ""
        } else if(text.toLowerCase().contains("let's")){
            var restOfSentence = text.toLowerCase().substring(5)
            var responses = arrayOf("Yeah let's", "It'll be great if we", "Yes, we should", "Do you all agree that we should", "hmm")
            var num = (Math.random()*responses.size).toInt()
            text = responses[num] + restOfSentence
            if (restOfSentence.equals("")||restOfSentence.equals(" "))
                text = ""
        } else if(text.toLowerCase().contains("i don't think")){
            var restOfSentence = text.toLowerCase().substring(13)
            var responses = arrayOf("We can't think", "Maybe", "I'd be impressed if", "Hmm", "Well,")
            var num = (Math.random()*responses.size).toInt()
            text = responses[num] + restOfSentence
            if (restOfSentence.equals("")||restOfSentence.equals(" ") || text.toLowerCase().indexOf("i don't think so") == 0)
                text = ""
        } else if(text.toLowerCase().contains("we can")){
            var restOfSentence = text.toLowerCase().substring(6)
            var responses = arrayOf("mhmm we can", "Yup let's", "I don't think we will", "I believe we will be able to")
            var num = (Math.random()*responses.size).toInt()
            text = responses[num] + restOfSentence
            if (restOfSentence.equals("")||restOfSentence.equals(" "))
                text = ""
        } else if(text.toLowerCase().contains("right")){
            var restOfSentence = text.toLowerCase().substring(5)
            var responses = arrayOf("mhmm", "Yup", "uh huh")
            var num = (Math.random()*responses.size).toInt()
            text = responses[num]
            if (restOfSentence.equals("")||restOfSentence.equals(" "))
                text = ""
        }/*else if(text.toLowerCase().contains("i think")){
            var num = (Math.random()*2).toInt()
            var responses = arrayOf("Yeah", "That's worth considering", "I like that idea", "Mhmm", "I agree")
        } else if(text.toLowerCase().contains("i feel like")){
            var num = (Math.random()*2).toInt()
            var responses = arrayOf("Yup", "Good idea", "Oh, I see", "Exactly", "hmmm")
        } else if(text.toLowerCase().contains("i'm pretty sure")){
            var num = (Math.random()*2).toInt()
            var responses = arrayOf("mhmm", "possibly", "that makes sense", "uh huh")
        } else if(text.toLowerCase().contains("i don't know")){
            var num = (Math.random()*2).toInt()
            var responses = arrayOf("", "", "", "", "")
        } else if(text.toLowerCase().contains("who is")){
            var num = (Math.random()*2).toInt()
            var responses = arrayOf("I know who", "Uhh", "Hmm", "umm")
        } else if(text.toLowerCase().contains("robot know")){
            var num = (Math.random()*2).toInt()
            var responses = arrayOf("Jibo knows everything", "Yup", "I do know", "Umm")
        } else if(text.toLowerCase().contains("i wonder if")){
            var num = (Math.random()*2).toInt()
            var responses = arrayOf("Hmm", "Interesting", "That's worth considering", "Definitely")
        } else if(text.toLowerCase().contains("let's")){
            var num = (Math.random()*2).toInt()
            var responses = arrayOf("Yup", "Sounds good", "Yes, we should", "Do you all agree that we should", "hmm")
        } else if(text.toLowerCase().contains("i don't think we")){
            var num = (Math.random()*2).toInt()
            var responses = arrayOf("", "", "", "", "")
        } else if(text.toLowerCase().contains("we can")){
            var num = (Math.random()*2).toInt()
            var responses = arrayOf("mhmm", "Yup", "Let's try it", "Definitely", "uh huh")
        } else if(text.toLowerCase().contains("right")){
            var num = (Math.random()*2).toInt()
            var responses = arrayOf("mhmm", "Yup", "uh huh")
        }*//*else if(text.toLowerCase().contains("i'm going to fail")){

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
        }*/

        mCommandLibrary?.say(text, this)
        Thread.sleep(2000)
        onListenClick()
    }

    override fun onParseError() {}

}
