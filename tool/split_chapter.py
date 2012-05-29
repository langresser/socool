import re,os;

def split_file(file):
    fp = open(file, 'r');
    dir_name = file[:file.rfind('.')];

    if not os.path.exists(dir_name):
        os.makedirs(dir_name);

    all_text = [];
    data_text = [];

    current_title = "";
    for line in fp:
        if re.search('.*半部.*第.*集.*第.*章', line):
            current_title = line.strip();

            if len(data_text) > 0:
                all_text.append(data_text);

            data_text = [];
            data_text.append(line);
            continue;

        data_text.append(line);
    fp.close();

    fpwd = open('{0}/data.txt'.format(dir_name), 'w');
    

    i = 0;
    for each in all_text:
        i += 1;
        fpw = open('{0}/{1}.txt'.format(dir_name, i), 'w');
        fpw.writelines(each);
        fpw.close();

        size = 0;
        for eee in each:
            size += len(eee);

        fpwd.write('{0}.txt@@{1}@@{2}@@{3}'.format(i, len(each), size, each[0]));

    fpwd.close();
split_file('无限恐怖.txt');
