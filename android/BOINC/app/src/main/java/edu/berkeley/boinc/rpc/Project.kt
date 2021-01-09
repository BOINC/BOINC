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
package edu.berkeley.boinc.rpc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Project(
    var masterURL: String = "",
    var projectDir: String = "",
    var resourceShare: Float = 0f,
    var projectName: String = "",
    var userName: String = "",
    var teamName: String = "",
    var hostVenue: String = "",
    var hostId: Int = 0,
    val guiURLs: MutableList<GuiUrl?> = mutableListOf(),
    var userTotalCredit: Double = 0.0,
    var userExpAvgCredit: Double = 0.0,
    /**
     * As reported by server
     */
    var hostTotalCredit: Double = 0.0,
    /**
     * As reported by server
     */
    var hostExpAvgCredit: Double = 0.0,
    var diskUsage: Double = 0.0,
    var noOfRPCFailures: Int = 0,
    var masterFetchFailures: Int = 0,
    /**
     * Earliest time to contact any server
     */
    var minRPCTime: Double = 0.0,
    var downloadBackoff: Double = 0.0,
    var uploadBackoff: Double = 0.0,
    var cpuShortTermDebt: Double = 0.0,
    var cpuLongTermDebt: Double = 0.0,
    var cpuBackoffTime: Double = 0.0,
    var cpuBackoffInterval: Double = 0.0,
    var cudaDebt: Double = 0.0,
    var cudaShortTermDebt: Double = 0.0,
    var cudaBackoffTime: Double = 0.0,
    var cudaBackoffInterval: Double = 0.0,
    var atiDebt: Double = 0.0,
    var atiShortTermDebt: Double = 0.0,
    var atiBackoffTime: Double = 0.0,
    var atiBackoffInterval: Double = 0.0,
    var durationCorrectionFactor: Double = 0.0,
    /**
     * Need to contact scheduling server. Encodes the reason for the request.
     */
    var scheduledRPCPending: Int = 0,
    var projectFilesDownloadedTime: Double = 0.0,
    var lastRPCTime: Double = 0.0,
    /**
     * Need to fetch and parse the master URL
     */
    var masterURLFetchPending: Boolean = false,
    var nonCPUIntensive: Boolean = false,
    var suspendedViaGUI: Boolean = false,
    var doNotRequestMoreWork: Boolean = false,
    var schedulerRPCInProgress: Boolean = false,
    var attachedViaAcctMgr: Boolean = false,
    var detachWhenDone: Boolean = false,
    var ended: Boolean = false,
    var trickleUpPending: Boolean = false,
    var noCPUPref: Boolean = false,
    var noCUDAPref: Boolean = false,
    var noATIPref: Boolean = false
) : Parcelable {
    val name: String
        get() = if (projectName.isEmpty()) masterURL else projectName

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return other is Project && other.resourceShare.compareTo(resourceShare) == 0 && hostId == other.hostId &&
                other.userTotalCredit.compareTo(userTotalCredit) == 0 && other.userExpAvgCredit.compareTo(userExpAvgCredit) == 0 &&
                other.hostTotalCredit.compareTo(hostTotalCredit) == 0 && other.hostExpAvgCredit.compareTo(hostExpAvgCredit) == 0 &&
                other.diskUsage.compareTo(diskUsage) == 0 && noOfRPCFailures == other.noOfRPCFailures &&
                masterFetchFailures == other.masterFetchFailures && other.minRPCTime.compareTo(minRPCTime) == 0 &&
                other.downloadBackoff.compareTo(downloadBackoff) == 0 && other.uploadBackoff.compareTo(uploadBackoff) == 0 &&
                other.cpuShortTermDebt.compareTo(cpuShortTermDebt) == 0 && other.cpuLongTermDebt.compareTo(cpuLongTermDebt) == 0 &&
                other.cpuBackoffTime.compareTo(cpuBackoffTime) == 0 && other.cpuBackoffInterval.compareTo(cpuBackoffInterval) == 0 &&
                other.cudaDebt.compareTo(cudaDebt) == 0 && other.cudaShortTermDebt.compareTo(cudaShortTermDebt) == 0 &&
                other.cudaBackoffTime.compareTo(cudaBackoffTime) == 0 && other.cudaBackoffInterval.compareTo(cudaBackoffInterval) == 0 &&
                other.atiDebt.compareTo(atiDebt) == 0 && other.atiShortTermDebt.compareTo(atiShortTermDebt) == 0 &&
                other.atiBackoffTime.compareTo(atiBackoffTime) == 0 && other.atiBackoffInterval.compareTo(atiBackoffInterval) == 0 &&
                other.durationCorrectionFactor.compareTo(durationCorrectionFactor) == 0 &&
                masterURLFetchPending == other.masterURLFetchPending && scheduledRPCPending == other.scheduledRPCPending &&
                nonCPUIntensive == other.nonCPUIntensive && suspendedViaGUI == other.suspendedViaGUI &&
                doNotRequestMoreWork == other.doNotRequestMoreWork && schedulerRPCInProgress == other.schedulerRPCInProgress &&
                attachedViaAcctMgr == other.attachedViaAcctMgr && detachWhenDone == other.detachWhenDone &&
                ended == other.ended && trickleUpPending == other.trickleUpPending &&
                other.projectFilesDownloadedTime.compareTo(projectFilesDownloadedTime) == 0 &&
                other.lastRPCTime.compareTo(lastRPCTime) == 0 && noCPUPref == other.noCPUPref &&
                noCUDAPref == other.noCUDAPref && noATIPref == other.noATIPref &&
                masterURL.equals(other.masterURL, ignoreCase = true) && projectDir == other.projectDir &&
                projectName == other.projectName && userName.equals(other.userName, ignoreCase = true) &&
                teamName == other.teamName && hostVenue == other.hostVenue && guiURLs == other.guiURLs
    }

    override fun hashCode() = Objects.hash(masterURL.toLowerCase(Locale.ROOT), projectDir, resourceShare,
            projectName, userName.toLowerCase(Locale.ROOT), teamName, hostVenue, hostId, guiURLs,
            userTotalCredit, userExpAvgCredit, hostTotalCredit, hostExpAvgCredit, diskUsage,
            noOfRPCFailures, masterFetchFailures, minRPCTime, downloadBackoff, uploadBackoff,
            cpuShortTermDebt, cpuLongTermDebt, cpuBackoffTime, cpuBackoffInterval, cudaDebt,
            cudaShortTermDebt, cudaBackoffTime, cudaBackoffInterval, atiDebt, atiShortTermDebt,
            atiBackoffTime, atiBackoffInterval, durationCorrectionFactor, masterURLFetchPending,
            scheduledRPCPending, nonCPUIntensive, suspendedViaGUI, doNotRequestMoreWork,
            schedulerRPCInProgress, attachedViaAcctMgr, detachWhenDone, ended, trickleUpPending,
            projectFilesDownloadedTime, lastRPCTime, noCPUPref, noCUDAPref, noATIPref)

    object Fields {
        const val PROJECT_DIR = "project_dir"
        const val RESOURCE_SHARE = "resource_share"
        const val USER_NAME = "user_name"
        const val TEAM_NAME = "team_name"
        const val HOST_VENUE = "host_venue"
        const val HOSTID = "hostid"
        const val USER_TOTAL_CREDIT = "user_total_credit"
        const val USER_EXPAVG_CREDIT = "user_expavg_credit"
        const val HOST_TOTAL_CREDIT = "host_total_credit"
        const val HOST_EXPAVG_CREDIT = "host_expavg_credit"
        const val NRPC_FAILURES = "nrpc_failures"
        const val MASTER_FETCH_FAILURES = "master_fetch_failures"
        const val MIN_RPC_TIME = "min_rpc_time"
        const val DOWNLOAD_BACKOFF = "download_backoff"
        const val UPLOAD_BACKOFF = "upload_backoff"
        const val CPU_BACKOFF_TIME = "cpu_backoff_time"
        const val CPU_BACKOFF_INTERVAL = "cpu_backoff_interval"
        const val CUDA_DEBT = "cuda_debt"
        const val CUDA_SHORT_TERM_DEBT = "cuda_short_term_debt"
        const val CUDA_BACKOFF_TIME = "cuda_backoff_time"
        const val CUDA_BACKOFF_INTERVAL = "cuda_backoff_interval"
        const val ATI_DEBT = "ati_debt"
        const val ATI_SHORT_TERM_DEBT = "ati_short_term_debt"
        const val ATI_BACKOFF_TIME = "ati_backoff_time"
        const val ATI_BACKOFF_INTERVAL = "ati_backoff_interval"
        const val DURATION_CORRECTION_FACTOR = "duration_correction_factor"
        const val MASTER_URL_FETCH_PENDING = "master_url_fetch_pending"
        const val SCHED_RPC_PENDING = "sched_rpc_pending"
        const val SUSPENDED_VIA_GUI = "suspended_via_gui"
        const val DONT_REQUEST_MORE_WORK = "dont_request_more_work"
        const val SCHEDULER_RPC_IN_PROGRESS = "scheduler_rpc_in_progress"
        const val ATTACHED_VIA_ACCT_MGR = "attached_via_acct_mgr"
        const val DETACH_WHEN_DONE = "detach_when_done"
        const val ENDED = "ended"
        const val TRICKLE_UP_PENDING = "trickle_up_pending"
        const val PROJECT_FILES_DOWNLOADED_TIME = "project_files_downloaded_time"
        const val LAST_RPC_TIME = "last_rpc_time"
        const val NO_CPU_PREF = "no_cpu_pref"
        const val NO_CUDA_PREF = "no_cuda_pref"
        const val NO_ATI_PREF = "no_ati_pref"
    }
}
