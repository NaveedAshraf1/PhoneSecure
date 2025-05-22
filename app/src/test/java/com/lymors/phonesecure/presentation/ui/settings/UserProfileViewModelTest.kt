package com.lymors.phonesecure.presentation.ui.settings

import com.lymors.phonesecure.domain.model.EmergencyContact
import com.lymors.phonesecure.domain.model.User
import com.lymors.phonesecure.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileViewModelTest {

    private lateinit var viewModel: UserProfileViewModel
    private lateinit var userRepository: UserRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mock()
        viewModel = UserProfileViewModel(userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        val initialState = viewModel.uiState.first()
        assertTrue(initialState is UserProfileUiState.Loading)
    }

    @Test
    fun `loads user profile successfully`() = runTest {
        // Given
        val user = User(
            id = UUID.randomUUID().toString(),
            name = "John Doe",
            email = "john@example.com",
            phoneNumber = "1234567890",
            emergencyContacts = emptyList()
        )
        whenever(userRepository.getCurrentUser()).thenReturn(user)

        // When
        viewModel = UserProfileViewModel(userRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state is UserProfileUiState.Success)
        assertEquals(user, (state as UserProfileUiState.Success).user)
    }

    @Test
    fun `handles error when loading user profile`() = runTest {
        // Given
        whenever(userRepository.getCurrentUser()).thenThrow(RuntimeException("Error"))

        // When
        viewModel = UserProfileViewModel(userRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state is UserProfileUiState.Error)
        assertEquals("Failed to load user profile", (state as UserProfileUiState.Error).message)
    }

    @Test
    fun `saves user profile successfully`() = runTest {
        // Given
        val name = "John Doe"
        val email = "john@example.com"
        val phone = "1234567890"
        whenever(userRepository.saveUser(any())).thenReturn(true)

        // When
        viewModel.saveUserProfile(name, email, phone)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(userRepository).saveUser(any())
        val state = viewModel.uiState.first()
        assertTrue(state is UserProfileUiState.Success)
    }

    @Test
    fun `handles error when saving user profile`() = runTest {
        // Given
        whenever(userRepository.saveUser(any())).thenReturn(false)

        // When
        viewModel.saveUserProfile("name", "email", "phone")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state is UserProfileUiState.Error)
        assertEquals("Failed to save user profile", (state as UserProfileUiState.Error).message)
    }

    @Test
    fun `adds emergency contact successfully`() = runTest {
        // Given
        val name = "Emergency Contact"
        val phone = "9876543210"
        whenever(userRepository.addEmergencyContact(any())).thenReturn(true)
        whenever(userRepository.getCurrentUser()).thenReturn(
            User(
                id = UUID.randomUUID().toString(),
                name = "John Doe",
                email = "john@example.com",
                phoneNumber = "1234567890",
                emergencyContacts = listOf(EmergencyContact(name = name, phoneNumber = phone))
            )
        )

        // When
        viewModel.addEmergencyContact(name, phone)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(userRepository).addEmergencyContact(any())
        val state = viewModel.uiState.first()
        assertTrue(state is UserProfileUiState.Success)
    }

    @Test
    fun `handles error when adding emergency contact`() = runTest {
        // Given
        whenever(userRepository.addEmergencyContact(any())).thenReturn(false)

        // When
        viewModel.addEmergencyContact("name", "phone")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state is UserProfileUiState.Error)
        assertEquals("Failed to add emergency contact", (state as UserProfileUiState.Error).message)
    }

    @Test
    fun `deletes emergency contact successfully`() = runTest {
        // Given
        val contactId = UUID.randomUUID().toString()
        whenever(userRepository.deleteEmergencyContact(contactId)).thenReturn(true)
        whenever(userRepository.getCurrentUser()).thenReturn(
            User(
                id = UUID.randomUUID().toString(),
                name = "John Doe",
                email = "john@example.com",
                phoneNumber = "1234567890",
                emergencyContacts = emptyList()
            )
        )

        // When
        viewModel.deleteEmergencyContact(contactId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(userRepository).deleteEmergencyContact(contactId)
        val state = viewModel.uiState.first()
        assertTrue(state is UserProfileUiState.Success)
    }

    @Test
    fun `handles error when deleting emergency contact`() = runTest {
        // Given
        whenever(userRepository.deleteEmergencyContact(any())).thenReturn(false)

        // When
        viewModel.deleteEmergencyContact("id")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.first()
        assertTrue(state is UserProfileUiState.Error)
        assertEquals("Failed to delete emergency contact", (state as UserProfileUiState.Error).message)
    }
}
