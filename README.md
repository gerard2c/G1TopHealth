# TopHealth

![Screenshot 2025-06-26 133603](https://github.com/user-attachments/assets/70cfdd77-1845-48fe-a187-ea276fced5a4)

This study highlights the development of the TopHealth mobile application designed to make healthcare more accessible through digital appointment booking, information access, and service customization. The app simplifies health management for users by integrating user-friendly interfaces, support for various health cards, and doctor scheduling in real-time. It also features admin functionalities for healthcare providers to manage appointments and patient records.

## Overview

TopHealth is a mobile health service application developed for Android devices. It provides users with a smooth and intuitive way to book medical appointments, browse wellness services, and manage their health-related data—all from their smartphones. The application also includes an administrator side for managing doctors’ schedules and booking confirmations.

## Requirements

* Android Studio (Electric Eel or newer) installed on Mac, Linux, or Windows
* Java Development Kit (JDK 8 or higher)
* Gradle Build System (automatically included with Android Studio)
* Android device (API level 21 or higher) with developer mode and USB debugging enabled
* USB cable to connect Android device to your computer
* Internet connection for real-time features and data access (if applicable)
* Firebase (optional): for login authentication, real-time database, or cloud messaging
* Image assets in JPG or PNG format for profile upload and UI graphics

## Features

### 1. Onboarding Experience

![Screenshot 2025-06-26 133603](https://github.com/user-attachments/assets/2fe7c1af-f897-4b88-8848-2a4d5bce796d)

* A slideshow with real doctor imagery introduces app features.
* Message: “Book your Doctor any time, anywhere.”
* Ends with a **GET STARTED** button directing to the login/register screen.

### 2. Easy Login & Secure Registration

![Screenshot 2025-06-26 133450](https://github.com/user-attachments/assets/64ea9927-62a9-4420-851d-e0caed13bddd)

* Login with username and password.
* Password recovery via phone/email.
* New user registration with form validation.
* Agreement to Terms and Privacy Policy is required.

### 3. Home Dashboard with Health Services

![Screenshot 2025-06-26 133726](https://github.com/user-attachments/assets/7a339369-02cc-4640-911d-3f608284cde7)

* Interactive cards for services like:

  * Derma Center
  * Corporate Health Packages
  * HMO Coverage
  * Walk-in Options
* Call-to-action buttons: "Book Now", "Know More", "Discover Services", etc.

### 4. Appointment Booking System

![Screenshot 2025-06-26 134409](https://github.com/user-attachments/assets/8c448fb3-49fa-429f-b42d-d7a30bc780b6)

* Confirm personal information.
* Choose check-up type (OB-GYN, Pediatrics, Internal Medicine, etc.)
* Select healthcare card (e.g., Maxicare, Intellicare).
* Enter card/account number if applicable.
* Choose doctor and time slot based on availability.

### 5. Profile and Settings Customization

![Screenshot 2025-06-26 143814](https://github.com/user-attachments/assets/3a6dda19-24bd-4472-b89b-4699c9a6b331)
![Screenshot 2025-06-26 143850](https://github.com/user-attachments/assets/7ea30f28-bab2-40fa-a21b-1ed6a61474c5)
![Screenshot 2025-06-26 143641](https://github.com/user-attachments/assets/0d4e4a53-c966-4d1a-b9f2-f6fd799e004f)
![Screenshot 2025-06-26 143656](https://github.com/user-attachments/assets/dda547c0-f96b-4a9d-9801-98bddf685e77)


* Update personal details (name, contact, profile image).
* Security options:

  * Change password
  * Biometrics login
  * Two-Factor Authentication (2FA)
  * Session management
  * Security alerts

### 6. Contact and Support Access

![Screenshot 2025-06-26 135041](https://github.com/user-attachments/assets/b129eb9c-54b5-432b-a74e-3baa51f32b3a)

* FAQ, support submission, feedback collection
* Direct “Contact Us” section

### 7. Archives (User)

![Screenshot 2025-06-26 143855](https://github.com/user-attachments/assets/adfbff81-30d7-4b84-8918-24e71b4f9cc9)

* View confirmed and unconfirmed appointments from the past 30 days
* Organized view of health history

---

## Admin Features

### 8. Admin Dashboard

![Screenshot 2025-06-26 144015](https://github.com/user-attachments/assets/69f1b395-d815-4fd2-b248-bbffbd22f2f5)
![Screenshot 2025-06-26 144024](https://github.com/user-attachments/assets/45dcede2-daa9-44bf-a51e-7503f5de477a)
![Screenshot 2025-06-26 144018](https://github.com/user-attachments/assets/c6345058-4893-4bee-87b1-32a8ca898c3e)

* View, confirm, reschedule, or decline appointments
* Add/edit available doctor time slots
* Manage healthcare professionals
* Notification on appointment status updates

### 9. Archives (Admin)

![Screenshot 2025-06-26 144039](https://github.com/user-attachments/assets/fdb24913-8e65-4e9a-966c-2293c2501eef)

* Filter appointments by:

  * Date (Today, Last 7 Days, Last 30 Days)
  * Status (Confirmed, Unconfirmed)
* View appointment details including:

  * Patient name, check-up type, healthcare provider, doctor, time
  * System-generated appointment ID

### 10. Analytics Dashboard

![Screenshot 2025-06-26 144048](https://github.com/user-attachments/assets/988ddf5d-d952-4f0b-80e9-2316a10bdbed)

* **Confirmation Status** – Doughnut chart of appointment states
* **Appointments Over Last 7 Days** – Line graph
* **Most Popular Doctor** – Based on appointment volume
* **Most Popular Check-up Types**
* **Most Demanding Time Slot**

---

## Demo Video

[Click this URL for TopHealth Features Demo](https://drive.google.com/file/d/1uXmf_HODgucPcmyf8HR_hkCB0OPoySCv/view?usp=sharing)

## Researchers/Developers:

* Russel Ford D. Manila
* Pauline Kaye V. Jao
* Gerard G. Escueta

## Adviser:

* Jefferson A. Costales

## School Name:

Eulogio "Amang" Rodriguez Institute of Science and Technology
Nagtahan, Sampaloc, Manila, Philippines
College of Computing Studies
IT Department

## Course:

* Bachelor of Science in Information Technology

## Date:

* June 26, 2025
