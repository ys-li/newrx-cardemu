package tech.enchor.newrxcard

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import android.support.v4.view.accessibility.AccessibilityEventCompat.setAction
import android.content.Intent
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import org.liquidplayer.javascript.*
import org.liquidplayer.service.MicroService
import tech.enchor.newrxcard.Utils.Companion.hexStringToByteArray
import java.net.URI
import java.nio.charset.Charset


class HostCardEmulatorService: HostApduService() {


    companion object {
        val TAG = "Host Card Emulator"
        val STATUS_SUCCESS = "9000"
        val STATUS_WRONG_PIN = "9004"
        val STATUS_FAILED = "6F00"
        val CRYPTO_SUCCESS = "DEDE"
        val CLA_NOT_SUPPORTED = "6E00"
        val INS_NOT_SUPPORTED = "6D00"
        val AID = "A0000002471001"
        val SELECT_INS = "A4"
        val CRYPTO_INS = "AE"
        val GET_KEY_ROOT_INS = "C0"
        val LIMITED_P1 = "97"
        val SUBSCRIPTIVE_P1 = "98"
        val FULL_P1 = "99"
        val DEFAULT_CLA = "00"
        val MIN_APDU_LENGTH = 12
        val NEWEST_PIN = "1234"
        val FIRST_PIN = "5678"
    }

    var service : MicroService? = null

    var processing = false
    var temp_signed = ""
    var temp_address = ""


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NFC", "Started service")



        service = MicroService(
                applicationContext,
                URI("android.resource://tech.enchor.newrxcard/raw/test"),
                MicroService.ServiceStartListener { service, synchronizer ->
                    Log.d("nfc-js", "running script")


                    service.addEventListener("console", MicroService.EventListener{
                        service, event, payload ->
                        printMsg(payload.getString("content"), "mam")
                    })

                    service.addEventListener("signed_content", MicroService.EventListener{
                        service, event, payload ->
                        processing = false
                        temp_signed = payload.getString("payload")
                        temp_address = payload.getString("address")
                        printMsg("signature length: ${temp_signed.length} trytes, address length: ${temp_address.length}", "mam")
                        printMsg("address: " + temp_address.substring(0,10) + "..." + temp_address.substring(temp_address.length-10), "mam")
                        printMsg("payload: " + temp_signed.substring(0,10) + "..." + temp_signed.substring(temp_signed.length-10), "mam")
                        //printMsg("debug-decoded: ${payload.getString("decoded")}")

                    })

                    service.addEventListener("newest_root", MicroService.EventListener{
                        service, event, payload ->
                        val root = payload.getString("root")
                        val sideKey = payload.getString("sideKey")
                        sendResponseApdu(root.toByteArray(Charsets.US_ASCII) + sideKey.toByteArray(Charsets.US_ASCII) + Utils.hexStringToByteArray(STATUS_SUCCESS))

                        printMsg("newest root: ${root.substring(0,10)}...${root.substring(root.length-10)}", "mam")
                        printMsg("sideKey: $sideKey", "mam")
                        printMsg("-----------------------------------------")
                        printMsg("LIMITED: SENT NEWEST ROOT AND SIDEKEY")
                        printMsg("-----------------------------------------")
                    })

                    service.addEventListener("first_root", MicroService.EventListener{
                        service, event, payload ->
                        val root = payload.getString("root")
                        val sideKey = payload.getString("sideKey")
                        sendResponseApdu(root.toByteArray(Charsets.US_ASCII) + sideKey.toByteArray(Charsets.US_ASCII) + Utils.hexStringToByteArray(STATUS_SUCCESS))

                        printMsg("first root: ${root.substring(0,10)}...${root.substring(root.length-10)}", "mam")
                        printMsg("sideKey: $sideKey", "mam")

                        printMsg("-----------------------------------------")
                        printMsg("FULL: SENT FIRST ROOT AND SIDEKEY")
                        printMsg("-----------------------------------------")
                    })

                }
        )

        service?.start()





        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: " + reason)
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray? {

        processing = true

        if(service == null) {
            return Utils.hexStringToByteArray(STATUS_FAILED)
        }

        if (commandApdu == null) {
            printMsg("read fail")
            return Utils.hexStringToByteArray(STATUS_FAILED)
        }

        val hexCommandApdu = Utils.toHex(commandApdu)


        if (hexCommandApdu.length < MIN_APDU_LENGTH) {
            return Utils.hexStringToByteArray(STATUS_FAILED)
        }

        if (hexCommandApdu.substring(0, 2) != DEFAULT_CLA) {
            return Utils.hexStringToByteArray(CLA_NOT_SUPPORTED)
        }

        val ins = hexCommandApdu.substring(2, 4)
        printMsg("received INS: $ins")

        when (ins){
            SELECT_INS -> {

                printMsg("-----------------------------------------")
                printMsg("NEW - SELECTED - A4")
                printMsg("-----------------------------------------")

                if (hexCommandApdu.substring(10, 24) == AID)  {
                    printMsg("SELECTED...")
                    return Utils.hexStringToByteArray(STATUS_SUCCESS)
                } else {
                    return Utils.hexStringToByteArray(STATUS_FAILED)
                }
            }
            CRYPTO_INS -> {
                val p1 = hexCommandApdu.substring(4, 6)
                val p2 = hexCommandApdu.substring(6, 8)
                val large = hexCommandApdu.substring(8, 10) == "00"



                val cmdBodySize : Int
                val content : String
                if (large){
                    cmdBodySize = hexCommandApdu.substring(10, 14).toInt(16)
                    content = hexCommandApdu.substring(14, hexCommandApdu.length - 4)
                } else {
                    cmdBodySize = hexCommandApdu.substring(8, 10).toInt(16)
                    content = hexCommandApdu.substring(10, hexCommandApdu.length - 4)
                }

                printMsg("purported size: $cmdBodySize, real size: ${content.length / 2}")

                val content_ascii = hexStringToByteArray(content).toString(Charsets.US_ASCII)

                printMsg("msg to be signed: ${content_ascii.substring(0, 50)}...")


                val payload = JSONObject()
                payload.put("msg", content_ascii)

                service?.emit("sign_content", payload)

                while (processing){
                    Thread.sleep(30)
                }



                //return Utils.hexStringToByteArray("9000000000900000000090000000009000000000900000000090000000009000000000900000000090000000009000000000900000000090000000009000000000900000000090000000009000000000900000000090000000009000000000")

                //DEBUG
                //temp_signed = temp_signed.substring(0,100)

                printMsg("length of signature: ${temp_signed.length}")


                printMsg("-----------------------------------------")
                printMsg("PROCESSED SUCCESSFULLY")
                printMsg("-----------------------------------------")
                //DEBUG ENDS

                val returnBytes = temp_address.toByteArray(Charsets.US_ASCII) + temp_signed.toByteArray(Charsets.US_ASCII) + Utils.hexStringToByteArray(STATUS_SUCCESS)

                temp_signed = ""
                temp_address = ""

                return returnBytes

            }
            GET_KEY_ROOT_INS -> {

                printMsg("GET_KEY_ROOT - cmd: ${hexCommandApdu}")
                val p1 = hexCommandApdu.substring(4, 6)
                val p2 = hexCommandApdu.substring(6, 8)
                val large = hexCommandApdu.substring(8, 10) == "00"



                val cmdBodySize : Int
                val content : String
                if (large){
                    cmdBodySize = hexCommandApdu.substring(10, 14).toInt(16)
                    content = hexCommandApdu.substring(14, hexCommandApdu.length - 4)
                } else {
                    cmdBodySize = hexCommandApdu.substring(8, 10).toInt(16)
                    content = hexCommandApdu.substring(10, hexCommandApdu.length - 4)
                }

                printMsg("purported size: $cmdBodySize, real size: ${content.length / 2}")

                val content_ascii = hexStringToByteArray(content).toString(Charsets.US_ASCII)

                printMsg("received pin: $content_ascii")

                when (p1){
                    FULL_P1 -> {
                        if (content_ascii == FIRST_PIN){
                            service?.emit("get_first_root_wsk")
                            return null
                        } else {
                            printMsg("-----------------------------------------")
                            printMsg("WRONG PIN")
                            printMsg("-----------------------------------------")
                            return Utils.hexStringToByteArray(STATUS_WRONG_PIN)
                        }
                    }
                    LIMITED_P1 -> {
                        if (content_ascii == NEWEST_PIN){
                            service?.emit("get_newest_root_wsk")
                            return null
                        } else {
                            printMsg("-----------------------------------------")
                            printMsg("WRONG PIN")
                            printMsg("-----------------------------------------")
                            return Utils.hexStringToByteArray(STATUS_WRONG_PIN)
                        }
                    }
                }

            }
        }


        if (hexCommandApdu.substring(2, 4) != SELECT_INS) {
            return Utils.hexStringToByteArray(INS_NOT_SUPPORTED)
        }


        return Utils.hexStringToByteArray(STATUS_FAILED)


    }

    fun printMsg(s : String, t: String = "NFC"){
        Log.d("NFC", s)
        val local = Intent()
        local.action = "tech.enchor.newrxcard"
        local.putExtra("console", s)
        local.putExtra("tag", t)
        this.sendBroadcast(local)
    }
}