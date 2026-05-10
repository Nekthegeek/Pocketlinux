package com.pocketlinux.de.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.pocketlinux.de.R
import com.pocketlinux.de.bootstrap.DistroProfile

class DistroPickerFragment : Fragment() {

    interface Listener {
        fun onInstallRequested(profile: DistroProfile)
        fun onStartRequested(profile: DistroProfile)
    }

    private var listener: Listener? = null

    fun setListener(l: Listener) { listener = l }

    // Track which profiles have been bootstrapped this session
    private val installedProfiles = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_distro_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DistroProfile.entries.forEachIndexed { index, profile ->
            val cardRes = when (index) {
                0 -> R.id.card0
                1 -> R.id.card1
                2 -> R.id.card2
                else -> R.id.card3
            }
            val card = view.findViewById<View>(cardRes)
            card.findViewById<TextView>(R.id.tvDistroName).text = profile.displayName
            card.findViewById<TextView>(R.id.tvDistroDesc).text = profile.description
            refreshCard(card, profile)
        }
    }

    private fun refreshCard(card: View, profile: DistroProfile) {
        val btnInstall = card.findViewById<Button>(R.id.btnInstall)
        val btnStart = card.findViewById<Button>(R.id.btnStart)
        val installed = profile.id in installedProfiles
        btnInstall.visibility = if (installed) View.GONE else View.VISIBLE
        btnStart.visibility = if (installed) View.VISIBLE else View.GONE
        btnInstall.setOnClickListener {
            installedProfiles.add(profile.id)
            refreshCard(card, profile)
            listener?.onInstallRequested(profile)
        }
        btnStart.setOnClickListener { listener?.onStartRequested(profile) }
    }

    fun markInstalled(profileId: String) {
        installedProfiles.add(profileId)
        view?.let { root ->
            DistroProfile.entries.forEachIndexed { index, profile ->
                if (profile.id == profileId) {
                    val cardRes = when (index) {
                        0 -> R.id.card0; 1 -> R.id.card1; 2 -> R.id.card2; else -> R.id.card3
                    }
                    refreshCard(root.findViewById(cardRes), profile)
                }
            }
        }
    }
}
