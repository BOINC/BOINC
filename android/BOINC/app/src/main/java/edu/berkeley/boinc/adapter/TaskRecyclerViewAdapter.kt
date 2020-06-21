/*
 * This file is part of BOINC.
 * http://boinc.berkeley.edu
 * Copyright (C) 2020 University of California
 *
 * BOINC is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * BOINC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with BOINC.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.berkeley.boinc.adapter

import android.app.Dialog
import android.graphics.Bitmap
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import edu.berkeley.boinc.BOINCActivity
import edu.berkeley.boinc.R
import edu.berkeley.boinc.TasksFragment
import edu.berkeley.boinc.TasksFragment.TaskData
import edu.berkeley.boinc.databinding.DialogConfirmBinding
import edu.berkeley.boinc.databinding.TasksLayoutListItemBinding
import edu.berkeley.boinc.rpc.RpcClient
import edu.berkeley.boinc.utils.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

class TaskRecyclerViewAdapter(
        private val fragment: TasksFragment,
        private val taskList: MutableList<TasksFragment.TaskData>
) : RecyclerView.Adapter<TaskRecyclerViewAdapter.ViewHolder>() {
    private val elapsedTimeStringBuilder = StringBuilder()
    private val percentNumberFormat = NumberFormat.getPercentInstance().apply { minimumFractionDigits = 3 }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TasksLayoutListItemBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(binding)
    }

    override fun getItemCount() = taskList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.root.setOnClickListener {
            val taskData = taskList[position]
            taskData.isExpanded = !taskData.isExpanded
            notifyDataSetChanged()
        }

        val item = taskList[position]
        val finalIconId = holder.projectIcon.tag as String?

        if (item.id != finalIconId) {
            val icon = getIcon(position)
            if (icon == null) {
                holder.projectIcon.setImageDrawable(AppCompatResources.getDrawable(fragment.requireContext(),
                        R.drawable.ic_boinc))
            } else {
                holder.projectIcon.setImageBitmap(icon)
                holder.projectIcon.tag = item.id
            }
        }

        val result = item.result

        holder.header.text = result.app!!.displayName

        var tempProjectName: String? = result.projectURL
        val project = result.project
        if (project != null) {
            tempProjectName = project.name
            if (result.isProjectSuspendedViaGUI) {
                tempProjectName += " " + fragment.getString(R.string.tasks_header_project_paused)
            }
        }
        holder.projectName.text = tempProjectName

        holder.status.text = determineStatusText(item)

        if (result.state in arrayOf(RESULT_ABORTED, RESULT_COMPUTE_ERROR, RESULT_FILES_DOWNLOADING,
                        RESULT_FILES_UPLOADED, RESULT_FILES_UPLOADING, RESULT_READY_TO_REPORT,
                        RESULT_UPLOAD_FAILED)) {
            holder.statusPercentage.visibility = View.GONE
        } else {
            holder.statusPercentage.visibility = View.VISIBLE
            holder.statusPercentage.text = percentNumberFormat.format(result.fractionDone)
        }

        // --- end of independent view elements

        // progress bar: show when task active or expanded
        // result and process state are overlapping, e.g. PROCESS_EXECUTING and RESULT_FILES_DOWNLOADING
        // therefore check also whether task is active
        val active = item.isTaskActive && item.determineState() == PROCESS_EXECUTING
        if (active || item.isExpanded) {
            holder.progressBar.visibility = View.VISIBLE
            holder.progressBar.isIndeterminate = false
            holder.progressBar.progressDrawable = AppCompatResources.getDrawable(fragment.requireContext(),
                    R.drawable.progressbar)
            holder.progressBar.progress = (item.result.fractionDone * holder.progressBar.max).roundToInt()
        } else {
            holder.progressBar.visibility = View.GONE
        }

        if (!item.isExpanded) {
            // view is collapsed
            holder.expandButton.setImageResource(R.drawable.ic_baseline_keyboard_arrow_right)
            holder.rightColumnExpandWrapper.visibility = View.GONE
            holder.centerColumnExpandWrapper.visibility = View.GONE
        } else {
            // view is expanded
            holder.expandButton.setImageResource(R.drawable.ic_baseline_keyboard_arrow_down)
            holder.rightColumnExpandWrapper.visibility = View.VISIBLE
            holder.centerColumnExpandWrapper.visibility = View.VISIBLE

            // elapsed time
            // show time depending whether task is active or not
            val elapsedTime = if (item.result.isActiveTask) {
                item.result.elapsedTime.toLong() //is 0 when task finished
            } else {
                item.result.finalElapsedTime.toLong()
            }
            holder.time.text = DateUtils.formatElapsedTime(elapsedTimeStringBuilder, elapsedTime)

            // set deadline
            val deadlineDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(
                    item.result.reportDeadline), ZoneId.systemDefault())
            val deadline = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .format(deadlineDateTime)
            holder.deadline.text = deadline
            // set application friendly name
            if (result.app != null) {
                holder.taskName.text = item.result.name
            }

            // buttons
            if (item.determineState() == PROCESS_ABORTED) { //dont show buttons for aborted task
                holder.rightColumnExpandWrapper.visibility = View.INVISIBLE
            } else {
                if (item.nextState == -1) { // not waiting for new state
                    holder.suspendResumeButton.setOnClickListener(item.iconClickListener)
                    holder.requestProgressBar.visibility = View.GONE
                    val theme = fragment.requireActivity().theme

                    // checking what suspendResume button should be shown
                    when {
                        item.result.isSuspendedViaGUI -> { // show play
                            holder.suspendResumeButton.visibility = View.VISIBLE
                            holder.suspendResumeButton.setBackgroundColor(ResourcesCompat.getColor(fragment.resources,
                                    R.color.dark_green, theme))
                            holder.suspendResumeButton.setImageResource(R.drawable.ic_baseline_play_arrow_white)
                            holder.suspendResumeButton.tag = RpcClient.RESULT_RESUME // tag on button specified operation triggered in iconClickListener
                        }
                        item.determineState() == PROCESS_EXECUTING -> { // show pause
                            holder.suspendResumeButton.visibility = View.VISIBLE
                            holder.suspendResumeButton.setBackgroundColor(ResourcesCompat.getColor(fragment.resources,
                                    R.color.dark_green, theme))
                            holder.suspendResumeButton.setImageResource(R.drawable.ic_baseline_pause_white)
                            holder.suspendResumeButton.tag = RpcClient.RESULT_SUSPEND // tag on button specified operation triggered in iconClickListener
                        }
                        else -> { // show nothing
                            holder.suspendResumeButton.visibility = View.GONE
                        }
                    }
                } else {
                    // waiting for a new state
                    holder.suspendResumeButton.visibility = View.INVISIBLE
                    holder.requestProgressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun getIcon(position: Int): Bitmap? {
        // try to get current client status from monitor
        return try {
            BOINCActivity.monitor!!.getProjectIcon(taskList[position].result.projectURL)
        } catch (e: Exception) {
            if (Logging.WARNING) {
                Log.w(Logging.TAG, "TasksListAdapter: Could not load data, clientStatus not initialized.")
            }
            null
        }
    }

    private fun determineStatusText(tmp: TaskData): String {
        //read status
        val status = tmp.determineState()

        // custom state
        if (status == RESULT_SUSPENDED_VIA_GUI) {
            return fragment.getString(R.string.tasks_custom_suspended_via_gui)
        }
        if (status == RESULT_PROJECT_SUSPENDED) {
            return fragment.getString(R.string.tasks_custom_project_suspended_via_gui)
        }
        if (status == RESULT_READY_TO_REPORT) {
            return fragment.getString(R.string.tasks_custom_ready_to_report)
        }

        //active state
        return if (tmp.result.isActiveTask) {
            when (status) {
                PROCESS_UNINITIALIZED -> fragment.getString(R.string.tasks_active_uninitialized)
                PROCESS_EXECUTING -> fragment.getString(R.string.tasks_active_executing)
                PROCESS_ABORT_PENDING -> fragment.getString(R.string.tasks_active_abort_pending)
                PROCESS_QUIT_PENDING -> fragment.getString(R.string.tasks_active_quit_pending)
                PROCESS_SUSPENDED -> fragment.getString(R.string.tasks_active_suspended)
                else -> {
                    if (Logging.WARNING) {
                        Log.w(Logging.TAG, "determineStatusText could not map: " + tmp.determineState())
                    }
                    ""
                }
            }
        } else {
            // passive state
            when (status) {
                RESULT_NEW -> fragment.getString(R.string.tasks_result_new)
                RESULT_FILES_DOWNLOADING -> fragment.getString(R.string.tasks_result_files_downloading)
                RESULT_FILES_DOWNLOADED -> fragment.getString(R.string.tasks_result_files_downloaded)
                RESULT_COMPUTE_ERROR -> fragment.getString(R.string.tasks_result_compute_error)
                RESULT_FILES_UPLOADING -> fragment.getString(R.string.tasks_result_files_uploading)
                RESULT_FILES_UPLOADED -> fragment.getString(R.string.tasks_result_files_uploaded)
                RESULT_ABORTED -> fragment.getString(R.string.tasks_result_aborted)
                RESULT_UPLOAD_FAILED -> fragment.getString(R.string.tasks_result_upload_failed)
                else -> {
                    if (Logging.WARNING) {
                        Log.w(Logging.TAG, "determineStatusText could not map: " + tmp.determineState())
                    }
                    ""
                }
            }
        }
    }

    fun abortTask(position: Int) {
        val task = taskList[position]
        val dialogBinding = DialogConfirmBinding.inflate(fragment.layoutInflater)
        val dialog = Dialog(fragment.requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
        }
        dialogBinding.title.setText(R.string.confirm_abort_task_title)
        dialogBinding.message.text = fragment.getString(R.string.confirm_abort_task_message, task.result.name)
        dialogBinding.confirm.setText(R.string.confirm_abort_task_confirm)
        dialogBinding.confirm.setOnClickListener {
            task.nextState = RESULT_ABORTED
            fragment.lifecycleScope.launch {
                fragment.performResultOperation(task.result.projectURL, task.result.name, RpcClient.RESULT_ABORT)
                taskList.removeAt(position)
                notifyDataSetChanged()
                dialog.dismiss()
            }
            Snackbar.make(fragment.requireActivity().findViewById<View>(R.id.drawer_layout),
                    R.string.task_aborted, Snackbar.LENGTH_SHORT).show()
        }
        dialogBinding.cancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    inner class ViewHolder(binding: TasksLayoutListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val root = binding.root
        val projectIcon = binding.projectIcon
        val header = binding.taskHeader
        val projectName = binding.projectName
        val status = binding.taskStatus
        val statusPercentage = binding.taskStatusPercentage
        val progressBar = binding.progressBar
        val rightColumnExpandWrapper = binding.rightColumnExpandWrapper
        val centerColumnExpandWrapper = binding.centerColumnExpandWrapper
        val expandButton = binding.expandCollapse
        val requestProgressBar = binding.requestProgressBar
        val time = binding.taskTime
        val taskName = binding.taskName
        val suspendResumeButton = binding.suspendResumeTask
        val deadline = binding.deadline
    }
}
