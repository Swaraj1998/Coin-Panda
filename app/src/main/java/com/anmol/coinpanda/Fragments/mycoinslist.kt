package com.anmol.coinpanda.Fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anmol.coinpanda.Adapters.GridnewAdapter
import com.anmol.coinpanda.Helper.Dbcoinshelper
import com.anmol.coinpanda.Interfaces.ItemClickListener
import com.anmol.coinpanda.Model.Allcoin
import com.anmol.coinpanda.Model.Sqlcoin
import com.anmol.coinpanda.R
import com.anmol.coinpanda.TweetsActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Created by anmol on 3/15/2018.
 */
class mycoinslist : Fragment(){
    private var coingrid: GridView?=null
    private lateinit var gridAdapter: GridnewAdapter
    lateinit var allcoins:MutableList<Allcoin>
    lateinit var itemClickListener : ItemClickListener
    var db = FirebaseFirestore.getInstance()
    var empty: TextView? = null
    val auth = FirebaseAuth.getInstance()
    val messaging = FirebaseMessaging.getInstance()
    var pgr :ProgressBar?=null
    var databaseReference: DatabaseReference?= null
    var dcb : Dbcoinshelper?=null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val vi = inflater.inflate(R.layout.mycoinslist, container, false)
        coingrid = vi.findViewById(R.id.coingrid)
        empty = vi.findViewById(R.id.empty)
        pgr = vi.findViewById(R.id.pgr)
        empty?.visibility = View.GONE
        allcoins = ArrayList()
        dcb = Dbcoinshelper(activity!!)
        val handler = Handler()
        handler.postDelayed({
            loaddata()
        },200)
        databaseReference = FirebaseDatabase.getInstance().reference
        coingrid?.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            if(activity!=null){
                val dialog = Dialog(activity)
                dialog.setContentView(R.layout.dialoglayout)
                val prg : ProgressBar? = dialog.findViewById(R.id.prgbr)
                val coinimg: ImageView? = dialog.findViewById(R.id.coinimg)
                val cn :TextView? = dialog.findViewById(R.id.cn)
                val cs:TextView? = dialog.findViewById(R.id.cs)
                val cp:TextView? = dialog.findViewById(R.id.cp)
                val atp:Button? = dialog.findViewById(R.id.atp)
                val portfoliolay: LinearLayout?= dialog.findViewById(R.id.portfoliolay)
                val viewtweet:Button?=dialog.findViewById(R.id.viewtweets)
                val notificationswitch: Switch?= dialog.findViewById(R.id.notification)
                val remove:Button? = dialog.findViewById(R.id.remove)
                atp?.visibility = View.VISIBLE
                portfoliolay?.visibility = View.GONE
                databaseReference!!.child("database").child(auth.currentUser!!.uid).child("topics").addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {

                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if(p0.exists()){
                            for(data in p0.children){
                                if (data.key!!.contains(allcoins[i].coinname!!) && data.child("coinname").value.toString().contains(allcoins[i].coin!!)) {
                                    val notify:Boolean = data.child("notify").value as Boolean
                                    if (!notify) {
                                        notificationswitch?.isChecked = false
                                    }
                                }
                            }
                        }
                    }

                })
                notificationswitch?.isChecked = true
                notificationswitch?.setOnCheckedChangeListener { _, b ->
                    if (b){
                        val map = HashMap<String,Any>()
                        map["notify"] = true
                        topicsearch(0,allcoins[i].coinname,allcoins[i].coin)
                        databaseReference!!.child("database").child(auth.currentUser!!.uid).child("portfolio").child(allcoins[i].coinname!!).updateChildren(map)
                    }
                    else{
                        databaseReference!!.child("database").child(auth.currentUser!!.uid).child("topics").addListenerForSingleValueEvent(object :ValueEventListener{
                            override fun onCancelled(p0: DatabaseError) {

                            }

                            override fun onDataChange(p0: DataSnapshot) {
                                if(p0.exists()){
                                    for(data in p0.children){
                                        if (data.key!!.contains(allcoins[i].coinname!!) && data.child("coinname").value == allcoins[i].coin) {
                                            databaseReference!!.child("database").child(auth.currentUser!!.uid).child("topics").child(data.key!!).removeValue().addOnSuccessListener {
                                                messaging.unsubscribeFromTopic(data.key)
                                                databaseReference!!.child("topics").child(data.key!!).addListenerForSingleValueEvent(object:ValueEventListener{
                                                    override fun onCancelled(p0: DatabaseError) {

                                                    }

                                                    override fun onDataChange(p0: DataSnapshot) {
                                                        val count:Long = p0.child("count").value as Long
                                                        if (count>0){
                                                            val map  = java.util.HashMap<String, Any>()
                                                            map["count"] = count - 1
                                                            databaseReference!!.child("topics").child(data.key!!).updateChildren(map)
                                                        }
                                                    }

                                                })


                                            }

                                        }
                                    }
                                }
                            }

                        })
                        val map = java.util.HashMap<String, Any>()
                        map["notify"] = false
                        databaseReference!!.child("database").child(auth.currentUser!!.uid).child("portfolio").child(allcoins[i].coinname!!).updateChildren(map)
                    }
                }

                val coins = dcb!!.readData()
                var j = 0
                while (j<coins.size){
                    if(allcoins[i].coinname == coins[j].coinname){
                        atp?.visibility = View.GONE
                        portfoliolay?.visibility = View.VISIBLE
                    }
                    j++
                }
//                db.collection("users").document(auth.currentUser!!.uid).collection("portfolio").get().addOnCompleteListener {task ->
//                    for(doc in task.result){
//                        if(doc.id == allcoins[i].coinname!!){
//                            atp?.visibility = View.GONE
//                            portfoliolay?.visibility = View.VISIBLE
//                        }
//                    }
//                }
                cn?.text = allcoins[i].coin
                cs?.text = allcoins[i].coinname
                cp?.text = "#"+ allcoins[i].coinpage
                val testurl = "https://twitter.com/" + allcoins[i].coinpage + "/profile_image?size=original"
                if(activity!=null){
                    println("testurlcoins$testurl")
                    Glide.with(activity).load(testurl).into(coinimg)
                }
                viewtweet?.setOnClickListener {
                    if(activity!=null){
                        val intent = Intent(activity, TweetsActivity::class.java)
                        intent.putExtra("coin", allcoins[i].coinname)
                        startActivity(intent)
                    }

                }
                remove?.setOnClickListener {
                    val sqlcoin = Sqlcoin(allcoins[i].coin, allcoins[i].coinname, allcoins[i].coinpage)
                    dcb!!.deleteCoin(sqlcoin)
                    databaseReference!!.child("database").child(auth.currentUser!!.uid).child("topics").addListenerForSingleValueEvent(object:ValueEventListener{
                        override fun onCancelled(p0: DatabaseError) {

                        }

                        override fun onDataChange(p0: DataSnapshot) {
                            if(p0!!.exists()){
                                for(data in p0.children){
                                    if(data.key!!.contains(allcoins[i].coinname!!) && data.child("coinname").value == allcoins[i].coin){
                                        databaseReference!!.child("database").child(auth.currentUser!!.uid).child("topics").child(data.key!!).removeValue().addOnCompleteListener {
                                            messaging.unsubscribeFromTopic(data.key)
                                            databaseReference!!.child("database").child(auth.currentUser!!.uid).child("portfolio").child(allcoins[i].coinname!!)
                                                    .removeValue().addOnSuccessListener {
                                                        if(activity!=null){
                                                            Toast.makeText(activity,"Removed from your Portfolio", Toast.LENGTH_SHORT).show()
                                                        }

                                                    }

                                            databaseReference!!.child("topics").child(data.key!!).addListenerForSingleValueEvent(object:ValueEventListener{
                                                override fun onCancelled(p0: DatabaseError) {

                                                }

                                                override fun onDataChange(p0: DataSnapshot) {
                                                    val count:Long = p0.child("count").value as Long
                                                    if (count>0){
                                                        val map  = java.util.HashMap<String, Any>()
                                                        map["count"] = count - 1
                                                        databaseReference!!.child("topics").child(data.key!!).updateChildren(map)
                                                    }
                                                }

                                            })

                                        }
                                    }
                                }
                            }
                        }

                    })

                    dialog.dismiss()

                }
                atp?.setOnClickListener {
                    //                val intent = Intent(activity,PaymentActivity::class.java)
//                intent.putExtra("coin",allcoins[i].coin)
//                startActivity(intent)
                    val sqlcoin = Sqlcoin(allcoins[i].coin, allcoins[i].coinname, allcoins[i].coinpage)

                    dcb!!.insertData(sqlcoin)
                    topicsearch(0, allcoins[i].coinname, allcoins[i].coin)
                    prg?.visibility = View.VISIBLE
                    atp.visibility = View.GONE
                    val map = java.util.HashMap<String, Any>()
                    map["coin"] = allcoins[i].coin.toString()
                    map["coinpage"] = allcoins[i].coinpage.toString()
                    map["coin_symbol"] = allcoins[i].coinname.toString()
                    databaseReference!!.child("database").child(auth.currentUser!!.uid).child("portfolio").child(allcoins[i].coinname!!).setValue(map).addOnCompleteListener {
                        if(activity!=null){
                            Toast.makeText(activity,"Added to your Portfolio", Toast.LENGTH_SHORT).show()
                        }
                    }
                    topicsearch(0, allcoins[i].coinname, allcoins[i].coin)
                    dialog.dismiss()
                }
                dialog.show()
            }

        }
        return vi

    }
    private fun removetopic(id: String) {

        db.collection("topics").document(id).get().addOnCompleteListener{task ->
            val documentSnapshot = task.result
            val count = documentSnapshot.getLong("count")
            if (count!!>0){
                val map  = java.util.HashMap<String, Any>()
                map["count"] = count - 1
                db.collection("topics").document(id).set(map)
            }
            else{

            }
        }
        db.collection("users").document(auth.currentUser!!.uid).collection("topics").document(id).delete()
    }
    private fun topicsearch(i: Int, coinname: String?, coin: String?) {
        databaseReference!!.child("topics").child(coinname + i.toString()).addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0!!.exists()){
                    val count:Long = p0.child("count").value as Long
                    if(count > 990){
                        topicsearch(i+1, coinname, coin)
                    }
                    else{
                        val map  = java.util.HashMap<String, Any>()
                        map["notify"] = true
                        map["coinname"] = coin!!
                        databaseReference!!.child("database").child(auth.currentUser!!.uid).child("topics").child(coinname + i.toString()).setValue(map)
                                .addOnCompleteListener {
                                    messaging.subscribeToTopic(coinname + i.toString())
                                    val countmap = java.util.HashMap<String, Any>()
                                    countmap["count"] = count+1
                                    countmap["coin_symbol"] = coinname.toString()
                                    databaseReference!!.child("topics").child(coinname + i.toString()).setValue(countmap)
                                }
                    }
                }
                else{
                    val map  = java.util.HashMap<String, Any>()
                    map["notify"] = true
                    map["coinname"] = coin!!
                    databaseReference!!.child("database").child(auth.currentUser!!.uid).child("topics").child(coinname + i.toString())
                            .setValue(map).addOnSuccessListener {
                                messaging.subscribeToTopic(coinname + i.toString())
                                val count = java.util.HashMap<String, Any>()
                                count["count"] = 1
                                count["coin_symbol"] = coinname.toString()
                                databaseReference!!.child("topics").child(coinname + i.toString()).setValue(count)
                            }
                }
            }

        })
    }

    private fun loaddata() {
        allcoins.clear()
        pgr?.visibility = View.VISIBLE
        if(activity!=null){
            val data = dcb!!.readData()
            allcoins = data
            if(!allcoins.isEmpty()){
                pgr?.visibility = View.GONE
                empty?.visibility = View.GONE
                gridAdapter = GridnewAdapter(activity!!, allcoins)
                gridAdapter.notifyDataSetChanged()
                coingrid?.adapter = gridAdapter
            }
            else{
                pgr?.visibility = View.GONE
                empty?.visibility = View.VISIBLE
            }
        }
//        db.collection("users").document(auth.currentUser!!.uid).collection("portfolio")
//                .get().addOnCompleteListener{
//                    task ->
//                    allcoins.clear()
//                    for(doc in task.result.documents){
//                        val coinname = doc.id
//                        val name = doc.getString("coin_name")
//                        val coinpage = doc.getString("coinPage")
//                        val allcoin = Allcoin(coinname,name,coinpage)
//                        allcoins.add(allcoin)
//                    }
//                    if(activity!=null){
//                        if(!allcoins.isEmpty()){
//                            pgr?.visibility = View.GONE
//                            empty?.visibility = View.GONE
//                            gridAdapter = GridnewAdapter(activity!!, allcoins)
//                            gridAdapter.notifyDataSetChanged()
//                            coingrid?.adapter = gridAdapter
//                        }
//                        else{
//                            pgr?.visibility = View.GONE
//                            empty?.visibility = View.VISIBLE
//                        }
//                    }
//
//                }
    }

}