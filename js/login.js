// ===============================
// Aventra - login.js
// Login + Register (2-step OTP) forms (uses helpers from app.js)
// ===============================
// Confirmed endpoints:
//   POST /users/login       {email, password}          -> {token}
//   POST /users/send-otp    {email}                     -> {message}
//   POST /users/verify-otp  {email, otp}                -> {message}
//   POST /users/register    User object (name,email,phone,password)
//   (register backend-side already checks OTP was verified for that email)

function setFieldError(input, message) {
    let errorEl = input.parentElement.querySelector(".field-error");

    if (!errorEl) {
        errorEl = document.createElement("span");
        errorEl.className = "field-error";
        input.parentElement.appendChild(errorEl);
    }

    if (message) {
        errorEl.innerText = message;
        errorEl.classList.add("show");
    } else {
        errorEl.classList.remove("show");
    }
}

if (isLoggedIn()) {
    window.location.href = "index.html";
}

// ---------- LOGIN ----------

const loginForm = document.getElementById("login-form");

if (loginForm) {
    loginForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const email = document.getElementById("email").value.trim();
        const password = document.getElementById("password").value;
        const loginBtn = document.getElementById("login-btn");

        loginBtn.disabled = true;
        loginBtn.innerText = "Logging in...";

        try {
            const data = await apiFetch("/users/login", {
                method: "POST",
                body: JSON.stringify({ email, password })
            });

            localStorage.setItem("token", data.token);

            showToast("Login successful!");
            setTimeout(() => (window.location.href = "index.html"), 800);

        } catch (error) {
            showToast(error.message || "Invalid email or password");
        } finally {
            loginBtn.disabled = false;
            loginBtn.innerText = "Login";
        }
    });
}

// ---------- REGISTER (2-step: send OTP -> verify OTP -> register) ----------

const registerForm = document.getElementById("register-form");

if (registerForm) {

    const stepDetails = document.getElementById("step-details");
    const stepOtp = document.getElementById("step-otp");
    const otpForm = document.getElementById("otp-form");
    const backToDetails = document.getElementById("back-to-details");

    let pendingUser = null; // form data yahan store hota hai jab tak OTP verify na ho

    // Step 1: form fill -> send-otp
    registerForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const name = document.getElementById("name").value.trim();
        const email = document.getElementById("email").value.trim();
        const phone = document.getElementById("phone").value.trim();
        const password = document.getElementById("password").value;
        const confirmPassword = document.getElementById("confirm-password").value;
        const confirmInput = document.getElementById("confirm-password");
        const sendOtpBtn = document.getElementById("send-otp-btn");

        setFieldError(confirmInput, "");

        if (password !== confirmPassword) {
            setFieldError(confirmInput, "Passwords match nahi kar rahe");
            return;
        }

        sendOtpBtn.disabled = true;
        sendOtpBtn.innerText = "Sending OTP...";

        try {
            await apiFetch("/users/send-otp", {
                method: "POST",
                body: JSON.stringify({ email })
            });

            pendingUser = { name, email, phone, password };

            document.getElementById("otp-email-display").innerText = email;
            stepDetails.style.display = "none";
            stepOtp.style.display = "block";

            showToast("OTP bhej diya gaya hai!");

        } catch (error) {
            showToast(error.message || "OTP bhejna fail ho gaya");
        } finally {
            sendOtpBtn.disabled = false;
            sendOtpBtn.innerText = "Send OTP";
        }
    });

    // Step 2: OTP verify -> register
    otpForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const otp = document.getElementById("otp").value.trim();
        const verifyBtn = document.getElementById("verify-otp-btn");

        if (!pendingUser) {
            showToast("Pehle details bharo");
            stepOtp.style.display = "none";
            stepDetails.style.display = "block";
            return;
        }

        verifyBtn.disabled = true;
        verifyBtn.innerText = "Verifying...";

        try {
            await apiFetch("/users/verify-otp", {
                method: "POST",
                body: JSON.stringify({ email: pendingUser.email, otp })
            });

            // OTP verified -> ab actual account create karo
            await apiFetch("/users/register", {
                method: "POST",
                body: JSON.stringify(pendingUser)
            });

            showToast("Account created! Please login.");
            setTimeout(() => (window.location.href = "login.html"), 900);

        } catch (error) {
            showToast(error.message || "OTP verify/register fail ho gaya");
        } finally {
            verifyBtn.disabled = false;
            verifyBtn.innerText = "Verify & Create Account";
        }
    });

    backToDetails.addEventListener("click", () => {
        stepOtp.style.display = "none";
        stepDetails.style.display = "block";
    });
}
