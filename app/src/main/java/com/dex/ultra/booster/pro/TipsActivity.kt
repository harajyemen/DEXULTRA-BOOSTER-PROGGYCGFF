package com.dex.ultra.booster.pro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dex.ultra.booster.pro.databinding.ActivityTipsBinding

class TipsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTipsBinding

    private val tips = listOf(
        TipItem(
            emoji = "⚡",
            title = "تفعيل وضع الأداء العالي",
            body = "اذهب إلى إعدادات الهاتف ← إدارة البطارية ← وضع الأداء العالي. يعطي دفع إضافي للمعالج أثناء اللعب.",
            category = "أداء"
        ),
        TipItem(
            emoji = "🌡️",
            title = "منع ارتفاع الحرارة",
            body = "أبعد الهاتف عن الشحن أثناء اللعب. استخدم مروحة تبريد خارجية. الحرارة الزائدة = لاق مضمون.",
            category = "أداء"
        ),
        TipItem(
            emoji = "📶",
            title = "WiFi vs بيانات الجوال",
            body = "WiFi 5GHz يعطي أفضل بنق من 4G في معظم الحالات. تجنب WiFi 2.4GHz في الأماكن المزدحمة.",
            category = "شبكة"
        ),
        TipItem(
            emoji = "🔧",
            title = "إعدادات الجرافيكس المثالية",
            body = "اضبط الجرافيكس على: جودة متوسطة، التأثيرات منخفضة، الظلال مغلقة، معدل الإطارات أعلى خيار. هذا يرفع FPS بشكل كبير.",
            category = "جرافيكس"
        ),
        TipItem(
            emoji = "🎮",
            title = "وضع عدم الإزعاج",
            body = "فعّل وضع عدم الإزعاج قبل اللعب لمنع الإشعارات من قطع اللعبة وسبب الـ lag.",
            category = "أداء"
        ),
        TipItem(
            emoji = "📱",
            title = "تحرير RAM قبل اللعب",
            body = "أغلق جميع التطبيقات الخلفية. استخدم زر التعزيز في DexUltra لتنظيف الذاكرة تلقائياً قبل تشغيل ببجي.",
            category = "ذاكرة"
        ),
        TipItem(
            emoji = "🎯",
            title = "حساسية هيدشوت",
            body = "للهيدشوت الاحترافي: حساسية 3P بين 90-120، ADS بين 45-55. جرب الإعداد المسبق 'هيدشوت' في DexUltra.",
            category = "حساسية"
        ),
        TipItem(
            emoji = "🌐",
            title = "اختيار الخادم المناسب",
            body = "دائماً العب على أقرب خادم. إذا بنقك فوق 80ms جرب تحسين DNS في صفحة تحسين البنق.",
            category = "شبكة"
        ),
        TipItem(
            emoji = "⚙️",
            title = "Shizuku للأداء القصوى",
            body = "مع تفعيل Shizuku يمكن لـ DexUltra تعديل معامل المعالج والذاكرة مباشرة، مما يعطي أداء أعلى بنسبة 15-20%.",
            category = "متقدم"
        ),
        TipItem(
            emoji = "🔋",
            title = "استهلاك البطارية",
            body = "ضع الهاتف على شحن 80% قبل اللعب. الشحن أثناء اللعب يرفع الحرارة. شاشة السطوع 70% توفر البطارية دون التأثير على الرؤية.",
            category = "بطارية"
        ),
        TipItem(
            emoji = "🎵",
            title = "تعطيل الموسيقى الخلفية",
            body = "أوقف مشغل الموسيقى والتطبيقات الصوتية الخلفية. تستهلك CPU بدون داعٍ وتسبب تقطيع.",
            category = "أداء"
        ),
        TipItem(
            emoji = "👆",
            title = "الـ 4 أصابع للاحتراف",
            body = "إعداد 4 أصابع مع جيرو هو الأمثل للاحترافيين. يتيح لك القطع والتصويب في نفس الوقت.",
            category = "تحكم"
        ),
        TipItem(
            emoji = "📡",
            title = "DNS للألعاب",
            body = "استخدم DNS مخصص للألعاب: 1.1.1.1 (Cloudflare) أو 8.8.8.8 (Google) بدلاً من DNS المزود الافتراضي.",
            category = "شبكة"
        ),
        TipItem(
            emoji = "🔄",
            title = "إعادة تشغيل اللعبة دورياً",
            body = "أعد تشغيل ببجي كل 3-4 ماتشات لتحرير الذاكرة المتراكمة وتجنب تراجع الأداء.",
            category = "أداء"
        ),
        TipItem(
            emoji = "🖥️",
            title = "دقة الشاشة المثالية",
            body = "إذا كانت اللعبة تسمح، استخدم دقة شاشة أقل مع معدل إطارات أعلى. الـ 120fps بدقة أقل أفضل من 60fps بدقة عالية.",
            category = "جرافيكس"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTipsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.tips_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.rvTips.layoutManager = LinearLayoutManager(this)
        binding.rvTips.adapter = TipsAdapter(tips)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

data class TipItem(
    val emoji: String,
    val title: String,
    val body: String,
    val category: String
)

class TipsAdapter(private val tips: List<TipItem>) :
    RecyclerView.Adapter<TipsAdapter.TipViewHolder>() {

    inner class TipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEmoji: TextView = itemView.findViewById(R.id.tvTipEmoji)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTipTitle)
        val tvBody: TextView = itemView.findViewById(R.id.tvTipBody)
        val tvCategory: TextView = itemView.findViewById(R.id.tvTipCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tip, parent, false)
        return TipViewHolder(view)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        val tip = tips[position]
        holder.tvEmoji.text = tip.emoji
        holder.tvTitle.text = tip.title
        holder.tvBody.text = tip.body
        holder.tvCategory.text = tip.category

        // Expand/collapse on tap
        val isExpanded = holder.tvBody.visibility == View.VISIBLE
        holder.itemView.setOnClickListener {
            holder.tvBody.visibility = if (isExpanded) View.GONE else View.VISIBLE
        }
    }

    override fun getItemCount() = tips.size
}
