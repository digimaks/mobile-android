// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.storagelogic.dao.type

interface StorageDao<T> {
    suspend fun store(value: T)
    suspend fun storeAll(values: List<T>)
    suspend fun retrieve(identifier: String): T?
    suspend fun retrieveAll(): List<T>
    suspend fun update(value: T)
    suspend fun delete(identifier: String)
    suspend fun deleteAll()
}