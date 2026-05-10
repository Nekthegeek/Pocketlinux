package com.pocketlinux.de.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.pocketlinux.de.R
import com.pocketlinux.de.bootstrap.DistroProfile
import com.pocketlinux.de.bootstrap.SessionLauncher
import com.pocketlinux.de.databinding.ActivityMainBinding
import com.pocketlinux.de.termux.TermuxStatus

class MainActivity : AppCompatActivity(), DistroPickerFragment.Listener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var termuxStatus: TermuxStatus
    private lateinit var launcher: SessionLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        termuxStatus = TermuxStatus(this)
        launcher = SessionLauncher(this)

        checkTermux()
    }

    override fun onResume() {
        super.onResume()
        checkTermux()
    }

    private fun checkTermux() {
        if (!termuxStatus.isInstalled()) {
            showTermuxNotInstalledBanner()
            return
        }
        if (!termuxStatus.hasRunCommandPermission()) {
            showPermissionNeededBanner()
            return
        }
        showDistroPicker()
    }

    private fun showTermuxNotInstalledBanner() {
        binding.bannerTermux.visibility = View.VISIBLE
        binding.bannerPermission.visibility = View.GONE
        binding.fragmentContainer.visibility = View.GONE

        binding.btnInstallTermux.setOnClickListener {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux/"))
            )
        }
    }

    private fun showPermissionNeededBanner() {
        binding.bannerTermux.visibility = View.GONE
        binding.bannerPermission.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.GONE

        binding.btnGrantPermission.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_dialog_title)
                .setMessage(R.string.permission_dialog_message)
                .setPositiveButton(R.string.open_settings) { _, _ ->
                    startActivity(
                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:com.termux")
                        }
                    )
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun showDistroPicker() {
        binding.bannerTermux.visibility = View.GONE
        binding.bannerPermission.visibility = View.GONE
        binding.fragmentContainer.visibility = View.VISIBLE

        if (supportFragmentManager.findFragmentByTag(TAG_PICKER) == null) {
            val frag = DistroPickerFragment()
            frag.setListener(this)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, frag, TAG_PICKER)
                .commit()
        }
    }

    // --- DistroPickerFragment.Listener ---

    override fun onInstallRequested(profile: DistroProfile) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.install_confirm_title, profile.displayName))
            .setMessage(getString(R.string.install_confirm_message, profile.sizeEstimate))
            .setPositiveButton(R.string.install) { _, _ ->
                launcher.installAndBootstrap(profile)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onStartRequested(profile: DistroProfile) {
        launcher.startVncServer(profile)
        // Give VNC server 3 seconds to start, then open viewer
        binding.root.postDelayed({
            startActivity(
                Intent(this, ViewerActivity::class.java).apply {
                    putExtra(ViewerActivity.EXTRA_PROFILE_ID, profile.id)
                }
            )
        }, 3000)
    }

    companion object {
        private const val TAG_PICKER = "distro_picker"
    }
}
