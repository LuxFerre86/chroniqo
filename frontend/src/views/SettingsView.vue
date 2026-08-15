<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import { getErrorMessage } from '../api/client'
import { changePassword, deleteAccount, fetchCountries, type CountryOption, updateProfile } from '../api/user'
import { useAuthStore } from '../stores/auth'

const workingDayOptions = [
  { label: 'Mon', value: 'MONDAY' },
  { label: 'Tue', value: 'TUESDAY' },
  { label: 'Wed', value: 'WEDNESDAY' },
  { label: 'Thu', value: 'THURSDAY' },
  { label: 'Fri', value: 'FRIDAY' },
  { label: 'Sat', value: 'SATURDAY' },
  { label: 'Sun', value: 'SUNDAY' }
]

const authStore = useAuthStore()
const router = useRouter()
const countries = ref<CountryOption[]>([])
const loadError = ref('')
const profileError = ref('')
const profileSuccess = ref('')
const passwordError = ref('')
const passwordSuccess = ref('')
const dangerError = ref('')

const profileForm = reactive({
  firstName: '',
  lastName: '',
  email: '',
  weeklyTargetHours: 40,
  workingDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'] as string[],
  countryCode: '',
  subdivisionCode: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const deletePassword = ref('')

const selectedCountry = computed(() => countries.value.find((country) => country.code === profileForm.countryCode) ?? null)
const subdivisions = computed(() => selectedCountry.value?.subdivisions ?? [])

watch(selectedCountry, (country) => {
  if (!country?.subdivisions.some((item) => item.code === profileForm.subdivisionCode)) {
    profileForm.subdivisionCode = ''
  }
})

function syncProfileForm() {
  const user = authStore.currentUser
  if (!user) {
    return
  }
  profileForm.firstName = user.firstName
  profileForm.lastName = user.lastName
  profileForm.email = user.email
  profileForm.weeklyTargetHours = user.weeklyTargetHours
  profileForm.workingDays = user.workingDays?.length ? [...user.workingDays] : [...profileForm.workingDays]
  profileForm.countryCode = user.countryCode ?? ''
  profileForm.subdivisionCode = user.subdivisionCode ?? ''
}

async function loadSettings() {
  try {
    await authStore.fetchMe(true)
    syncProfileForm()
    countries.value = await fetchCountries()
  } catch (error) {
    loadError.value = getErrorMessage(error, 'Could not load your settings.')
  }
}

async function saveProfile() {
  profileError.value = ''
  profileSuccess.value = ''

  if (profileForm.weeklyTargetHours < 0 || profileForm.weeklyTargetHours > 80) {
    profileError.value = 'Weekly target hours must be between 0 and 80.'
    return
  }
  if (!profileForm.workingDays.length) {
    profileError.value = 'Select at least one working day.'
    return
  }

  try {
    await updateProfile({
      firstName: profileForm.firstName,
      lastName: profileForm.lastName,
      weeklyTargetHours: Number(profileForm.weeklyTargetHours),
      workingDays: profileForm.workingDays,
      countryCode: profileForm.countryCode || null,
      subdivisionCode: profileForm.subdivisionCode || null
    })
    if (authStore.currentUser) {
      authStore.currentUser = {
        ...authStore.currentUser,
        firstName: profileForm.firstName,
        lastName: profileForm.lastName,
        weeklyTargetHours: Number(profileForm.weeklyTargetHours),
        workingDays: [...profileForm.workingDays],
        countryCode: profileForm.countryCode || null,
        subdivisionCode: profileForm.subdivisionCode || null
      }
    }
    profileSuccess.value = 'Profile updated successfully.'
  } catch (error) {
    profileError.value = getErrorMessage(error, 'Could not update your profile.')
  }
}

async function savePassword() {
  passwordError.value = ''
  passwordSuccess.value = ''

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordError.value = 'New passwords do not match.'
    return
  }

  try {
    await changePassword(passwordForm.oldPassword, passwordForm.newPassword)
    passwordSuccess.value = 'Password changed successfully.'
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (error) {
    passwordError.value = getErrorMessage(error, 'Could not change your password.')
  }
}

async function handleDeleteAccount() {
  dangerError.value = ''
  if (!deletePassword.value) {
    dangerError.value = 'Enter your current password to delete the account.'
    return
  }
  if (!window.confirm('Delete your account permanently? This cannot be undone.')) {
    return
  }
  try {
    await deleteAccount(deletePassword.value)
    authStore.clearSession()
    await router.push('/login')
  } catch (error) {
    dangerError.value = getErrorMessage(error, 'Could not delete the account.')
  }
}

watch(
  () => authStore.currentUser,
  () => syncProfileForm(),
  { immediate: true }
)

onMounted(() => {
  void loadSettings()
})
</script>

<template>
  <AppLayout>
    <div class="stack-lg">
      <p v-if="loadError" class="error-message">{{ loadError }}</p>

      <section class="card">
        <div class="section-header section-header--compact">
          <div>
            <p class="eyebrow">Profile</p>
            <h2>Personal settings</h2>
          </div>
        </div>

        <div class="form-grid">
          <label class="field">
            <span>First name</span>
            <input v-model="profileForm.firstName" class="input" type="text" />
          </label>
          <label class="field">
            <span>Last name</span>
            <input v-model="profileForm.lastName" class="input" type="text" />
          </label>
          <label class="field field--full">
            <span>Email</span>
            <input v-model="profileForm.email" class="input" type="email" readonly />
          </label>
          <label class="field">
            <span>Weekly target hours</span>
            <input v-model.number="profileForm.weeklyTargetHours" class="input" type="number" min="0" max="80" />
          </label>
          <div class="field field--full">
            <span>Working days</span>
            <div class="checkbox-grid">
              <label v-for="day in workingDayOptions" :key="day.value" class="checkbox-card">
                <input v-model="profileForm.workingDays" type="checkbox" :value="day.value" />
                <span>{{ day.label }}</span>
              </label>
            </div>
          </div>
          <label class="field">
            <span>Country</span>
            <select v-model="profileForm.countryCode" class="input">
              <option value="">No holiday calendar</option>
              <option v-for="country in countries" :key="country.code" :value="country.code">{{ country.name }}</option>
            </select>
          </label>
          <label class="field">
            <span>Subdivision</span>
            <select v-model="profileForm.subdivisionCode" class="input" :disabled="!subdivisions.length">
              <option value="">Nationwide only</option>
              <option v-for="subdivision in subdivisions" :key="subdivision.code" :value="subdivision.code">
                {{ subdivision.name }}
              </option>
            </select>
          </label>
        </div>

        <p v-if="profileError" class="error-message">{{ profileError }}</p>
        <p v-if="profileSuccess" class="success-message">{{ profileSuccess }}</p>
        <button class="btn btn-primary" type="button" @click="saveProfile">Save profile</button>
      </section>

      <section class="card">
        <div class="section-header section-header--compact">
          <div>
            <p class="eyebrow">Security</p>
            <h2>Change password</h2>
          </div>
        </div>

        <div class="form-grid">
          <label class="field">
            <span>Current password</span>
            <input v-model="passwordForm.oldPassword" class="input" type="password" />
          </label>
          <label class="field">
            <span>New password</span>
            <input v-model="passwordForm.newPassword" class="input" type="password" />
          </label>
          <label class="field field--full">
            <span>Confirm new password</span>
            <input v-model="passwordForm.confirmPassword" class="input" type="password" />
          </label>
        </div>

        <p v-if="passwordError" class="error-message">{{ passwordError }}</p>
        <p v-if="passwordSuccess" class="success-message">{{ passwordSuccess }}</p>
        <button class="btn btn-primary" type="button" @click="savePassword">Update password</button>
      </section>

      <section class="card danger-card">
        <div class="section-header section-header--compact">
          <div>
            <p class="eyebrow">Danger zone</p>
            <h2>Delete account</h2>
          </div>
        </div>
        <p class="muted">This permanently removes your account and tracking data.</p>
        <label class="field field--full">
          <span>Current password</span>
          <input v-model="deletePassword" class="input" type="password" />
        </label>
        <p v-if="dangerError" class="error-message">{{ dangerError }}</p>
        <button class="btn btn-danger" type="button" @click="handleDeleteAccount">Delete account</button>
      </section>
    </div>
  </AppLayout>
</template>
