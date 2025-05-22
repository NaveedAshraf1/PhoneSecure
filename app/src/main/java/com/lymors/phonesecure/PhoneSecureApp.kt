package com.lymors.phonesecure

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import com.lymors.phonesecure.data.repository.LocationRepositoryImpl
import com.lymors.phonesecure.data.repository.SecurityEventRepositoryImpl
import com.lymors.phonesecure.data.repository.UserRepositoryImpl
import com.lymors.phonesecure.domain.repository.LocationRepository
import com.lymors.phonesecure.domain.repository.SecurityEventRepository
import com.lymors.phonesecure.domain.repository.UserRepository
import com.lymors.phonesecure.domain.usecase.FakeShutdownUseCase
import com.lymors.phonesecure.domain.usecase.IntruderDetectionUseCase
import com.lymors.phonesecure.domain.usecase.LocationTrackingUseCase
import com.lymors.phonesecure.domain.usecase.PanicButtonUseCase
import com.lymors.phonesecure.domain.usecase.SimChangeUseCase

/**
 * Main application class that provides repositories and use cases
 */
class PhoneSecureApp : Application(), Configuration.Provider {
    
    // Repositories
    private lateinit var userRepository: UserRepository
    private lateinit var securityEventRepository: SecurityEventRepository
    private lateinit var locationRepository: LocationRepository
    
    // Use cases
    private lateinit var fakeShutdownUseCase: FakeShutdownUseCase
    private lateinit var intruderDetectionUseCase: IntruderDetectionUseCase
    private lateinit var locationTrackingUseCase: LocationTrackingUseCase
    private lateinit var panicButtonUseCase: PanicButtonUseCase
    private lateinit var simChangeUseCase: SimChangeUseCase
    
    companion object {
        private lateinit var instance: PhoneSecureApp
        
        fun getAppContext(): Context = instance.applicationContext
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize repositories
        initializeRepositories()
        
        // Initialize use cases
        initializeUseCases()
        
        // Initialize notification channels
        com.lymors.phonesecure.presentation.utils.NotificationUtils.createNotificationChannels(this)
        
        // Initialize WorkManager
        WorkManager.initialize(this, workManagerConfiguration)
    }
    
    private fun initializeRepositories() {
        userRepository = UserRepositoryImpl()
        securityEventRepository = SecurityEventRepositoryImpl()
        locationRepository = LocationRepositoryImpl()
    }
    
    private fun initializeUseCases() {
        fakeShutdownUseCase = FakeShutdownUseCase(userRepository, securityEventRepository)
        intruderDetectionUseCase = IntruderDetectionUseCase(userRepository, securityEventRepository, locationRepository)
        locationTrackingUseCase = LocationTrackingUseCase(userRepository, locationRepository, securityEventRepository)
        panicButtonUseCase = PanicButtonUseCase(userRepository, securityEventRepository, locationRepository)
        simChangeUseCase = SimChangeUseCase(userRepository, securityEventRepository, locationRepository)
    }
    
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }
    
    // Repository getters
    fun getUserRepository(): UserRepository = userRepository
    fun getSecurityEventRepository(): SecurityEventRepository = securityEventRepository
    fun getLocationRepository(): LocationRepository = locationRepository
    
    // Use case getters
    fun getFakeShutdownUseCase(): FakeShutdownUseCase = fakeShutdownUseCase
    fun getIntruderDetectionUseCase(): IntruderDetectionUseCase = intruderDetectionUseCase
    fun getLocationTrackingUseCase(): LocationTrackingUseCase = locationTrackingUseCase
    fun getPanicButtonUseCase(): PanicButtonUseCase = panicButtonUseCase
    fun getSimChangeUseCase(): SimChangeUseCase = simChangeUseCase
}
