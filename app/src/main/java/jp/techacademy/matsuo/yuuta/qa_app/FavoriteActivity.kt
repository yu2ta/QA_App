package jp.techacademy.matsuo.yuuta.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.renderscript.Sampler
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.AdapterView
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ServerValue
import android.widget.ArrayAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_favorite.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_favorite.listView
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class FavoriteActivity : AppCompatActivity() {

    private lateinit var qIdList : ArrayList<Map<String,String>>
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter

    //DB関連
    val mDatabaseReference = FirebaseDatabase.getInstance().reference
    val uid = FirebaseAuth.getInstance().currentUser!!.uid
    val favRef = mDatabaseReference.child(FavoritePATH).child(uid)


    private val mChildAddListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            val data = dataSnapshot.value as Map<String, String>
            qIdList.add(data)
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            Log.d("qaapp", "a")
        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            val data = dataSnapshot.value as Map<String, String>
            qIdList.remove(data)
            mQuestionArrayList.clear()
            mDatabaseReference.child(ContentsPATH).addValueEventListener(mValueEventListener)
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
        }

        override fun onCancelled(error: DatabaseError) {
        }
    }

    private val mValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            mQuestionArrayList.clear()

            for(qId in qIdList) {
                val q_genre = qId["genre"].toString()
                val q_id = qId["uid"].toString()

                val map = snapshot.child(q_genre).child(q_id).value as Map<String, String>

                val title = map["title"] ?: ""
                val body = map["body"] ?: ""
                val name = map["name"] ?: ""
                val uid = map["uid"] ?: ""
                val imageString = map["image"] ?: ""
                val bytes =
                    if (imageString.isNotEmpty()) {
                        Base64.decode(imageString, Base64.DEFAULT)
                    } else {
                        byteArrayOf()
                    }

                val answerArrayList = ArrayList<Answer>()
                val answerMap = map["answers"] as Map<String, String>?
                if (answerMap != null) {
                    for (key in answerMap.keys) {
                        val temp = answerMap[key] as Map<String, String>
                        val answerBody = temp["body"] ?: ""
                        val answerName = temp["name"] ?: ""
                        val answerUid = temp["uid"] ?: ""
                        val answer = Answer(answerBody, answerName, answerUid, key)
                        answerArrayList.add(answer)
                    }
                }

                val question = Question(title, body, name, uid, q_id,
                    q_genre.toInt(), bytes, answerArrayList)
                mQuestionArrayList.add(question)
            }
            mAdapter.notifyDataSetChanged()
        }

        override fun onCancelled(error: DatabaseError) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        //List関連
        qIdList = ArrayList<Map<String,String>>()
        mQuestionArrayList = ArrayList<Question>()
        mAdapter = QuestionsListAdapter(this)

        mAdapter.setQuestionArrayList(mQuestionArrayList)
        listView.adapter = mAdapter

        listView.setOnItemClickListener{parent, view, position, id ->
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }

        favRef.addChildEventListener(mChildAddListener)
        mDatabaseReference.child(ContentsPATH).addValueEventListener(mValueEventListener)
    }
}