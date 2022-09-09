package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.ArmingStatus;
import com.udacity.catpoint.security.data.SecurityRepository;
import com.udacity.catpoint.security.data.Sensor;
import com.udacity.catpoint.security.data.SensorType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
    @Captor
    ArgumentCaptor<Sensor> sensorArgumentCaptor;

    @Mock
    SecurityRepository securityRepository;

    @Mock
    ImageService imageService;

    @InjectMocks
    SecurityService securityService;

    @Test
    void addStatusListener() {
    }

    @Test
    void removeStatusListener() {
    }

    @Test
    void setAlarmStatus() {
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    @DisplayName("1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.")
    void changeSensorActivationStatus_AlarmIsArmedAndSensor2Active_AlarmStatus2PendingAlarm(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(new Sensor("", SensorType.DOOR), true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    @DisplayName("2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.")
    void changeSensorActivationStatus_AlarmIsArmedAndSensor2ActiveAndAlarmStatusIsPending_AlarmStatus2Alarm(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(new Sensor("", SensorType.DOOR), true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    @DisplayName("3. If pending alarm and all sensors are inactive, return to no alarm state.")
    void changeSensorActivationStatus_PendingAlarmAndAllSensorInactive_AlarmStatus2NoAlarm(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        var sensors = Set.of(
                new Sensor("", SensorType.DOOR, false),
                new Sensor("", SensorType.DOOR, false)
        );
        when(securityRepository.getSensors()).thenReturn(sensors);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensors.stream().findFirst().get(), true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensors.stream().findFirst().get(), false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @MethodSource("provideCensorChanges")
    @DisplayName("4. If alarm is active, change in sensor state should not affect the alarm state.")
    void changeSensorActivationStatus_AlarmIsActiveAndSensorChange_AlarmStatusDoesntChange(Sensor sensor, boolean active) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, active);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test
    @DisplayName("5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.")
    void changeSensorActivationStatus_ActivateSensorAlreadyActivatedAndPendingAlarm_AlarmStatus2Alarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(new Sensor("", SensorType.DOOR, true), true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    @DisplayName("6. If a sensor is deactivated while already inactive, make no changes to the alarm state.")
    void changeSensorActivationStatus_DeactivateSensorAlreadyDeactivated_AlarmStatusDoesntChange() {
        securityService.changeSensorActivationStatus(new Sensor("", SensorType.DOOR, false), false);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test
    @DisplayName("7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.")
    void processImage_ContainCatAndArmedHome_AlarmStatus2Alarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(new BufferedImage(1, 1, 1));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @MethodSource("provideTestMaterialCase8")
    @DisplayName("8. If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.")
    void processImage_NotContainCatAndSensorsNotActive_AlarmStatus2NoAlarm(Set<Sensor> sensors, int verifyCallChangeNoAlarm) {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.processImage(new BufferedImage(1, 1, 1));
        verify(securityRepository, times(verifyCallChangeNoAlarm)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    @DisplayName("9. If the system is disarmed, set the status to no alarm.")
    void setAlarmStatus_Disarmed_AlarmStatus2NoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    @DisplayName("10. If the system is armed, reset all sensors to inactive.")
    void setAlarmStatus_Armed_AllSensorsInactivated(ArmingStatus armingStatus) {
        when(securityRepository.getSensors()).thenReturn(Set.of(
                new Sensor("", SensorType.DOOR, false),
                new Sensor("", SensorType.DOOR, true)
        ));
        securityService.setArmingStatus(armingStatus);
        verify(securityRepository, times(2)).updateSensor(sensorArgumentCaptor.capture());
        sensorArgumentCaptor.getAllValues()
                .forEach(sensor -> assertEquals(false, sensor.getActive()));
    }

    @Test
    @DisplayName("11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.")
    void setArmingStatus_Disarmed2ArmedHomeAndContainCat_AlarmStatus2Alarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        securityService.setCatDetected(true);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    private static Stream<Arguments> provideCensorChanges() {
        return Stream.of(
                Arguments.of(new Sensor("", SensorType.DOOR, true), false),
                Arguments.of(new Sensor("", SensorType.DOOR, false), true)
        );
    }

    private static Stream<Arguments> provideTestMaterialCase8() {
        return Stream.of(
                Arguments.of(Set.of(
                        new Sensor("", SensorType.DOOR, false),
                        new Sensor("", SensorType.DOOR, false)
                ), 1),
                Arguments.of(Set.of(
                        new Sensor("", SensorType.DOOR, false),
                        new Sensor("", SensorType.DOOR, true)
                ), 0)
        );
    }
}