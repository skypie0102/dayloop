package com.shadowmonarchbooks.dayloop.review

import com.shadowmonarchbooks.dayloop.data.PackStore
import com.shadowmonarchbooks.dayloop.data.progress.ProgressDb
import com.shadowmonarchbooks.dayloop.data.progress.ProgressRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug-only fixture access; absent from candidate/release APKs. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReviewDependencies {
    fun store(): PackStore
    fun repo(): ProgressRepository
    fun db(): ProgressDb
}
