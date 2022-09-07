package id.co.sistema.vkey

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.vkey.android.internal.vguard.engine.BasicThreatInfo
import id.co.sistema.vkey.databinding.ActivityMainBinding
import id.co.sistema.vkey.sfio.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var threatAdapter: ThreatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        threatAdapter = ThreatAdapter()

        initClickListener()
        setupRecyclerView()
        initVersionSDK()
    }

    /**
     * Preparing click listener for all button.
     * All button is executed to move different activity
     * */
    private fun initClickListener() {
        val context = this
        binding.apply {
            // Move to Encrypting/Decrypting a Block of Data
            btnBlockData.setOnClickListener {
                startActivity(Intent(context, BlockDataActivity::class.java))
            }

            // Move to Encrypting/Decrypting a Block of Data to/from a File
            btnBlockDataFile.setOnClickListener {
                startActivity(Intent(context, BlockDataToFromFileActivity::class.java))
            }

            // Move to Encrypting/Decrypting a String to/from a File
            btnStringFile.setOnClickListener {
                startActivity(Intent(context, StringToFromFileActivity::class.java))
            }

            // Move to Encrypting an Existing File
            btnEncryptExistingFile.setOnClickListener {
                startActivity(Intent(context, ExistingTextFileActivity::class.java))
            }

            // Move to Encrypting an Existing Non Text File (Sample Image)
            btnEncryptFileNonText.setOnClickListener {
                startActivity(Intent(context, ExistingNonTextFileActivity::class.java))
            }

            // Move to Demo Secure Keyboard
            btnSecureKeyboard.setOnClickListener {
                startActivity(Intent(context, SecureKeyboardActivity::class.java))
            }
        }
    }

    /**
     * Prepare configuration for Recycler View (layout manager & adapter)
     * */
    private fun setupRecyclerView() {
        binding.rvThreats.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = threatAdapter
        }
    }

    /**
     * Fetch all version SDK to show it in Main Activity.
     * (tid, sdkVersion, firmwareVersion, processorVersion, firmwareReturnCode)
     * */
    private fun initVersionSDK() {
        CustomApplication.vGuardMgr?.let {
            binding.apply {
                tvTid.text = "TID: ${it.troubleshootingId}"
                tvValueSdkVersion.text = it.sdkVersion()
                tvValueFirmwareVersion.text = CustomApplication.vosWrapper?.firmwareVersion
                tvValueProcessorVersion.text = CustomApplication.vosWrapper?.processorVersion
                tvValueFirmwareCode.text = CustomApplication.firmwareReturnCode.toString()
            }
        }
    }

    /**
     * Method to retrieve [BasicThreatInfo] from [CustomApplication] class
     * after being broadcast by [EventBus].
     */
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onMessageEvent(threats: ArrayList<BasicThreatInfo>) {
        val message = "Threat onMessageEvent: $threats, threats size: ${threats.size}"
        showLog(message)
        binding.pbThreats.isVisible = false

        if (threats.isEmpty()) {
            binding.rvThreats.isVisible = false
            binding.tvTitleThreat.text = "No threats found"
            return
        }

        binding.rvThreats.isVisible = true
        threatAdapter.submitList(threats)
    }

    /**
     * Handle [EventBus] lifecycle.
     */
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    /**
     * Handle [EventBus] lifecycle.
     */
    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }
}