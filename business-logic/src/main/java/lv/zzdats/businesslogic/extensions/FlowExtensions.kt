// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn

fun <T> Flow<T>.safeAsync(with: (Throwable) -> (T)): Flow<T> {
    return this.flowOn(Dispatchers.IO).catch {
        if (it is CancellationException) throw it
        emit(with(it))
    }
}
