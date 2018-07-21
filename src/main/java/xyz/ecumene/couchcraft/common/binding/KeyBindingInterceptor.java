package xyz.ecumene.couchcraft.common.binding;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import xyz.ecumene.couchcraft.utils.FieldHelper;

public class KeyBindingInterceptor extends KeyBinding
{


    protected KeyBinding interceptedKeyBinding;
    private boolean interceptionActive;

    private int interceptedPressTime;

    static FieldHelper<KeyBinding, Boolean> fieldPressed = new FieldHelper<>(KeyBinding.class, "field_74513_e", "pressed");
    static FieldHelper<KeyBinding, Integer> fieldPressTime = new FieldHelper<>(KeyBinding.class, "field_151474_i", "pressTime");


    /**
     * Create an Interceptor based on an existing binding.
     * The initial interception mode is OFF.
     * If existingKeyBinding is already a KeyBindingInterceptor, a reinitialised copy will be created but no further effect.
     *
     * @param existingKeyBinding - the binding that will be intercepted.
     */
    public KeyBindingInterceptor(KeyBinding existingKeyBinding)
    {
        super(existingKeyBinding.getKeyDescription(), existingKeyBinding.getKeyCode(), existingKeyBinding.getKeyCategory());

        this.interceptionActive = false;

        try
        {
            fieldPressed.set(this, false);
            fieldPressTime.set(this, 0);
        }
        catch (IllegalAccessException e)
        {
            throw new ReportedException(new CrashReport("Exception updating KeyBindingInterceptor", e));
        }

        this.interceptedPressTime = 0;

        if (existingKeyBinding instanceof KeyBindingInterceptor)
        {
            interceptedKeyBinding = ((KeyBindingInterceptor) existingKeyBinding).getOriginalKeyBinding();
        }
        else
        {
            interceptedKeyBinding = existingKeyBinding;
        }

        KeyBinding.resetKeyBindingArrayAndHash();
    }

    public void setInterceptionActive(boolean newMode)
    {
        if (newMode && !interceptionActive)
        {
            this.interceptedPressTime = 0;
        }
        interceptionActive = newMode;
    }

    public boolean isKeyDown()
    {
        copyKeyCodeToOriginal();
        return interceptedKeyBinding.isPressed();
    }

    /**
     * @return returns false if interception isn't active.  Otherwise, retrieves one of the clicks (true) or false if no clicks left
     */
    public boolean retrieveClick()
    {
        try
        {
            copyKeyCodeToOriginal();
            if (interceptionActive)
            {
                copyClickInfoFromOriginal();

                if (this.interceptedPressTime == 0)
                {
                    return false;
                }
                else
                {
                    --this.interceptedPressTime;
                    return true;
                }
            }
            else
            {
                return false;
            }
        }
        catch (IllegalAccessException e)
        {
            throw new ReportedException(new CrashReport("Exception updating KeyBindingInterceptor", e));
        }
    }

    /**
     * A better name for this method would be retrieveClick.
     * If interception is on, resets .pressed and .pressTime to zero.
     * Otherwise, copies these from the intercepted KeyBinding.
     *
     * @return If interception is on, this will return false; Otherwise, it will pass on any clicks in the intercepted KeyBinding
     */
    @Override
    public boolean isPressed()
    {
        try
        {
            copyKeyCodeToOriginal();
            copyClickInfoFromOriginal();

            if (interceptionActive)
            {
                fieldPressed.set(this, false);
                fieldPressTime.set(this, 0);
                return false;
            }
            else
            {
                int pressTime = fieldPressTime.get(this);
                if (pressTime == 0)
                {
                    return false;
                }
                else
                {
                    fieldPressTime.set(this, --pressTime);
                    return true;
                }
            }
        }
        catch (IllegalAccessException e)
        {
            throw new ReportedException(new CrashReport("Exception updating KeyBindingInterceptor", e));
        }
    }

    public KeyBinding getOriginalKeyBinding()
    {
        return interceptedKeyBinding;
    }

    protected void copyClickInfoFromOriginal() throws IllegalAccessException
    {
        int pressTimeIntercepted = fieldPressTime.get(interceptedKeyBinding);
        int pressTime = fieldPressTime.get(this) + pressTimeIntercepted;
        this.interceptedPressTime += pressTimeIntercepted;
        fieldPressTime.set(interceptedKeyBinding, 0);
        fieldPressed.set(this, fieldPressed.get(interceptedKeyBinding));
        fieldPressTime.set(this, pressTime);
    }

    protected void copyKeyCodeToOriginal()
    {
        if (this.getKeyCode() != interceptedKeyBinding.getKeyCode())
        {
            setKeyCode(interceptedKeyBinding.getKeyCode());
            resetKeyBindingArrayAndHash();
        }
    }
}