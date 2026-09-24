// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.resourceslogic.di

import android.content.Context
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.resourceslogic.provider.ResourceProviderImpl
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("lv.zzdats.resourceslogic")
class ResourcesModule

@Single
fun provideResourceProvider(context: Context): ResourceProvider {
    return ResourceProviderImpl(context)
}