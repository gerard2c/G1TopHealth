package com.example.it3a_grp1_manila;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReminderWorker extends Worker {
    private static final String TAG = "ReminderWorker";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // Get the appointment ID and message from input data
            String appointmentId = getInputData().getString("appointmentId");
            String message = getInputData().getString("message");
            if (message == null) {
                message = "This is your appointment reminder!";
            }

            // Trigger the local notification using NotificationUtils
            NotificationUtils.sendLocalNotification(getApplicationContext(), "Appointment Reminder", message, appointmentId != null ? appointmentId : String.valueOf(System.currentTimeMillis() % 1000));

            Log.d(TAG, "Reminder notification sent for appointment: " + appointmentId);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in doWork: " + e.getMessage(), e);
            return Result.failure();
        }
    }
}