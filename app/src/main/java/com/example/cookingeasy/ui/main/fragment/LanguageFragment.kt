package com.example.cookingeasy.ui.main.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cookingeasy.common.locale.AppLocale
import com.example.cookingeasy.databinding.FragmentLanguageBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageFragment : Fragment() {

    private var _binding: FragmentLanguageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.rowLanguageSystem.setOnClickListener {
            selectLocale(AppLocale.TAG_SYSTEM)
        }
        binding.rowLanguageEnglish.setOnClickListener {
            selectLocale(AppLocale.TAG_ENGLISH)
        }
        binding.rowLanguageVietnamese.setOnClickListener {
            selectLocale(AppLocale.TAG_VIETNAMESE)
        }
        binding.rowLanguageJapanese.setOnClickListener {
            selectLocale(AppLocale.TAG_JAPANESE)
        }
        syncSelectionUi(AppLocale.currentTag())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun selectLocale(tag: String) {
        if (tag == AppLocale.currentTag()) {
            findNavController().popBackStack()
            return
        }
        AppLocale.apply(tag)
        findNavController().popBackStack()
        requireActivity().recreate()
    }

    private fun syncSelectionUi(selected: String) {
        binding.iconCheckSystem.visibility =
            if (selected == AppLocale.TAG_SYSTEM) View.VISIBLE else View.GONE
        binding.iconCheckEnglish.visibility =
            if (selected == AppLocale.TAG_ENGLISH) View.VISIBLE else View.GONE
        binding.iconCheckVietnamese.visibility =
            if (selected == AppLocale.TAG_VIETNAMESE) View.VISIBLE else View.GONE
        binding.iconCheckJapanese.visibility =
            if (selected == AppLocale.TAG_JAPANESE) View.VISIBLE else View.GONE
    }
}
