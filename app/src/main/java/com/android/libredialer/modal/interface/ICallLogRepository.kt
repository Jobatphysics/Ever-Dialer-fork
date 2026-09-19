package com.android.libredialer.modal.`interface`

import com.android.libredialer.modal.data.CallLogEntry

interface ICallLogRepository {
    fun getCallLogs(): List<CallLogEntry>
    fun deleteCallLog(entry: CallLogEntry): Boolean
    fun deleteCallLogs(entries: Collection<CallLogEntry>): Boolean
}