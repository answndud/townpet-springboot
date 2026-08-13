package com.townpet.identity;

record AccountTokenDeliveryRequested(
    AccountTokenPurpose purpose, String recipient, String encryptedToken) {}
