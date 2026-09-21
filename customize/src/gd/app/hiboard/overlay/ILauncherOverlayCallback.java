/*
 * Vendored Binder stubs matching DreamQuickGalance AIDL
 * (app/src/main/aidl/gd/app/hiboard/overlay/ILauncherOverlayCallback.aidl).
 * Soong does not compile that AIDL into this module; keep in sync manually.
 */
package gd.app.hiboard.overlay;
/** Callback from Quick Glance WindowServer to the launcher (OPPO/Heytap parity). */
public interface ILauncherOverlayCallback extends android.os.IInterface
{
  /** Default implementation for ILauncherOverlayCallback. */
  public static class Default implements gd.app.hiboard.overlay.ILauncherOverlayCallback
  {
    @Override public void overlayScrollChanged(float progress) throws android.os.RemoteException
    {
    }
    @Override public void overlayStatusChanged(int status) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements gd.app.hiboard.overlay.ILauncherOverlayCallback
  {
    /** Construct the stub and attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an gd.app.hiboard.overlay.ILauncherOverlayCallback interface,
     * generating a proxy if needed.
     */
    public static gd.app.hiboard.overlay.ILauncherOverlayCallback asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof gd.app.hiboard.overlay.ILauncherOverlayCallback))) {
        return ((gd.app.hiboard.overlay.ILauncherOverlayCallback)iin);
      }
      return new gd.app.hiboard.overlay.ILauncherOverlayCallback.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      switch (code)
      {
        case TRANSACTION_overlayScrollChanged:
        {
          float _arg0;
          _arg0 = data.readFloat();
          this.overlayScrollChanged(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_overlayStatusChanged:
        {
          int _arg0;
          _arg0 = data.readInt();
          this.overlayStatusChanged(_arg0);
          reply.writeNoException();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements gd.app.hiboard.overlay.ILauncherOverlayCallback
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      @Override public void overlayScrollChanged(float progress) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeFloat(progress);
          boolean _status = mRemote.transact(Stub.TRANSACTION_overlayScrollChanged, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void overlayStatusChanged(int status) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(status);
          boolean _status = mRemote.transact(Stub.TRANSACTION_overlayStatusChanged, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_overlayScrollChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_overlayStatusChanged = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "gd.app.hiboard.overlay.ILauncherOverlayCallback";
  public void overlayScrollChanged(float progress) throws android.os.RemoteException;
  public void overlayStatusChanged(int status) throws android.os.RemoteException;
}
