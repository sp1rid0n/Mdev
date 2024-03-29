package com.example.auctiontrainer.screens.createRoom

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auctiontrainer.base.AppData
import com.example.auctiontrainer.base.EventHandler
import com.example.auctiontrainer.base.LotModel
import com.example.auctiontrainer.base.SettingsRoom
import com.example.auctiontrainer.database.firebase.FbRoomsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random


sealed class CreateRoomViewState {
    data class Display(
        val items: MutableList<LotModel>,
        val settings: SettingsRoom
    ) : CreateRoomViewState()

    object NoItems: CreateRoomViewState()
    object Reload: CreateRoomViewState()
    object Success: CreateRoomViewState()
}

sealed class CreateRoomEvent {
    object EnterScreen : CreateRoomEvent()
    object SaveClick : CreateRoomEvent()
    data class DeleteClickable(val value: LotModel) : CreateRoomEvent()
    data class TimeSelected(val newValue: String) : CreateRoomEvent()
    data class CntTeamSelected(val newValue: String) : CreateRoomEvent()
}

@HiltViewModel
class CreateRoomViewModel @Inject constructor (
    private val data: AppData,
    private val roomsRepository: FbRoomsRepository
): ViewModel(), EventHandler<CreateRoomEvent> {

    private val _createRoomViewState: MutableLiveData<CreateRoomViewState> = MutableLiveData(CreateRoomViewState.NoItems)
    val createRoomViewState: LiveData<CreateRoomViewState> = _createRoomViewState

    override fun obtainEvent(event: CreateRoomEvent) {
        when (val currentState = _createRoomViewState.value) {
            is CreateRoomViewState.Display -> reduce(event, currentState)
            is CreateRoomViewState.NoItems -> fetchLot(currentState)
            is CreateRoomViewState.Reload -> fetchLot(currentState)
            else -> {}
        }
    }

    private fun reduce(event: CreateRoomEvent, currentState: CreateRoomViewState.Display) {
        when (event) {
            CreateRoomEvent.EnterScreen -> fetchLot(currentState)
            CreateRoomEvent.SaveClick -> saveData(currentState)
            is CreateRoomEvent.TimeSelected -> {
                val curCnt = currentState.settings.cntTeams
                data.setSettings(event.newValue.toInt(), curCnt)
                _createRoomViewState.postValue(
                    currentState.copy(settings = data.getSettings())
                )
            }
            is CreateRoomEvent.CntTeamSelected -> {
                val curTime = currentState.settings.time
                data.setSettings(curTime, event.newValue.toInt())
                _createRoomViewState.postValue(
                    currentState.copy(settings = data.getSettings())
                )
            }
            /*TODO*/
            // Пофиксить удаление
            is CreateRoomEvent.DeleteClickable -> {
                data.deleteLots(event.value)
                _createRoomViewState.postValue(
                    CreateRoomViewState.Reload
//                    currentState.copy(
//                        items = data.getLots(),
//                        settings = currentState.settings
//                    )
                )
            }
        }
    }

    private fun fetchLot(currentState: CreateRoomViewState) {
        val lots = data.getLots()
        if (lots.isEmpty()) {
            _createRoomViewState.postValue(CreateRoomViewState.NoItems)
        } else {
            _createRoomViewState.postValue(
                CreateRoomViewState.Display(lots, data.getSettings())
            )
        }
    }

    private fun saveData(currentState: CreateRoomViewState.Display) {
        viewModelScope.launch {
            try {
                val genCode = generationCode()
                data.setCode(genCode)
                roomsRepository.createRoom(genCode)
                _createRoomViewState.postValue(CreateRoomViewState.Success)
            } catch (e: Exception) {
                Log.e("save", e.toString())
            }
        }
    }

    /*TODO*/
    // Написать норм генератор
    private fun generationCode() : String {
        val length = 4

        val symbols = "abcdefghijklmnopqrstuvwxyz0123456789".toMutableList()
        var code = ""

        for (i in 0..length) {
            symbols.shuffle()
            code += symbols[Random.nextInt(symbols.size)]
        }
        return code
    }
}