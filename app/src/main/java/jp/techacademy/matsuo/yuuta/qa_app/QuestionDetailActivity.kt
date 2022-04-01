package jp.techacademy.matsuo.yuuta.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*


class QuestionDetailActivity : AppCompatActivity(), DatabaseReference.CompletionListener {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private var favFlag = 0
    private lateinit var mQuestionRef: DatabaseReference

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()


        //お気に入り登録のメソッド
        fabFab.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            val favRef = dataBaseReference.child(FavoritePATH).child(uid)

            if (favFlag == 0) {
                var data = HashMap<String,String>()
                data["genre"] = mQuestion.genre.toString()
                data["uid"] = mQuestion.questionUid
                favRef.push().setValue(data, this)
            } else {
                Log.d("qaapp", mQuestionRef.toString())
                mQuestionRef.removeValue()
            }
            favProcess()
        }

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                var intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
            favProcess()
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

        favProcess()
    }

    override fun onResume() {
        super.onResume()
        Log.d("qaapp", "from OnResume")
        favProcess()
    }

    //データ更新が成功したときの処理
    override fun onComplete(error: DatabaseError?, ref: DatabaseReference) {
    }

    private fun favProcess() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            fabFab.visibility = View.INVISIBLE
        } else {
            val uid = FirebaseAuth.getInstance().currentUser!!.uid
            val ref = FirebaseDatabase.getInstance().reference.child(FavoritePATH).child(uid)

            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val data = dataSnapshot.value as Map<*, *>?
                    var flag: Int = 0
                    var key: String = ""

                    if (data != null) {
                        for (map in data!!) {
                            var q = map.value as HashMap<String, String>
                            if(q.get("uid").equals(mQuestion.questionUid)) {
                                flag = 1
                                key = map.key.toString()
                                Log.d("qaapp", "flag on!")
                            }
                        }

                        if (flag == 1) {
                            favFlag = 1
                            mQuestionRef = ref.child(key)
                            fabFab.setImageResource(R.drawable.star_1)
                        } else {
                            favFlag = 0
                            fabFab.setImageResource(R.drawable.star_0)
                        }
                    } else {
                        favFlag = 0
                        fabFab.setImageResource(R.drawable.star_0)
                    }

                }

                override fun onCancelled(error: DatabaseError) {}
            })

        }
    }
}