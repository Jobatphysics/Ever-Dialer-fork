package com.android.libredialer

import com.android.libredialer.controller.CallLogViewModel
import com.android.libredialer.controller.ContactsViewModel
import com.android.libredialer.modal.`interface`.ICallLogRepository
import com.android.libredialer.modal.`interface`.IContactsRepository
import com.android.libredialer.modal.repository.CallLogRepository
import com.android.libredialer.modal.repository.ContactsRepository
import com.android.libredialer.controller.util.PreferenceManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<IContactsRepository> {
        ContactsRepository(androidContext().contentResolver, androidContext())
    }
    single {
        PreferenceManager(androidContext())
    }
    single<ICallLogRepository> {
        CallLogRepository(androidContext(), androidContext().contentResolver, get())
    }
    viewModel { ContactsViewModel(androidApplication(), get(), get()) }
    viewModel { CallLogViewModel(androidApplication(), get()) }
}