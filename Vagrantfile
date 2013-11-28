VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|

  config.vm.provision :shell, :path => "provision.sh"

  config.vm.box = 'precise64'
  config.vm.box_url = 'http://files.vagrantup.com/precise64.box'
  config.vm.network "forwarded_port", guest: 9000, host: 9000
  config.vm.network "forwarded_port", guest: 18080, host: 18080
  config.vm.synced_folder "~/.gu", "/home/vagrant/.gu"
  config.vm.synced_folder "~/.ivy2", "/home/vagrant/.ivy2"

  config.vm.provider "virtualbox" do |v|
    v.customize ["modifyvm", :id, "--memory", "3064"]
    v.customize ["modifyvm", :id, "--cpuexecutioncap", "80"]
  end

end