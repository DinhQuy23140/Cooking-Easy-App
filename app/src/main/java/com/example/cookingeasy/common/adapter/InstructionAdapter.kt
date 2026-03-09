import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cookingeasy.databinding.ItemInstructionTimelineBinding
import com.example.cookingeasy.domain.model.InstructionStep

class InstructionAdapter(
    private val steps: List<InstructionStep>
) : RecyclerView.Adapter<InstructionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemInstructionTimelineBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val binding = ItemInstructionTimelineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = steps.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val step = steps[position]

        holder.binding.tvStep.text = step.step.toString()
        holder.binding.tvInstruction.text = step.content

        // Ẩn line nếu là step cuối
        if (position == steps.lastIndex) {
            holder.binding.viewLine.visibility = View.INVISIBLE
        } else {
            holder.binding.viewLine.visibility = View.VISIBLE
        }
    }
}