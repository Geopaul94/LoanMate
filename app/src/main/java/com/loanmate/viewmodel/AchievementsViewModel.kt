package com.loanmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.data.local.AchievementEntity
import com.loanmate.data.repository.AchievementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementRepository: AchievementRepository
) : ViewModel() {

    val achievements: Flow<List<AchievementEntity>> = achievementRepository.getAllAchievements()

    init {
        viewModelScope.launch {
            achievementRepository.seedAchievements()
        }
    }
}
